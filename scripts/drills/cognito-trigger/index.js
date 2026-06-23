const crypto = require('crypto');
const {
  CognitoIdentityProviderClient,
  AdminAddUserToGroupCommand,
  AdminUpdateUserAttributesCommand,
} = require('@aws-sdk/client-cognito-identity-provider');

const client = new CognitoIdentityProviderClient({});

exports.handler = async (event) => {
  if (event.triggerSource === 'PostConfirmation_ConfirmSignUp') {
    const tenantId = crypto.randomUUID();
    const organizationName = event.request.userAttributes['custom:organization_name'] || 'My Organization';
    await client.send(new AdminUpdateUserAttributesCommand({
      UserPoolId: event.userPoolId,
      Username: event.userName,
      UserAttributes: [
        { Name: 'custom:tenant_id', Value: tenantId },
        { Name: 'custom:organization_name', Value: organizationName },
      ],
    }));
    await client.send(new AdminAddUserToGroupCommand({
      UserPoolId: event.userPoolId, Username: event.userName, GroupName: 'ADMIN',
    }));
  }
  if (event.triggerSource.startsWith('TokenGeneration_')) {
    const tenantId = event.request.userAttributes['custom:tenant_id'];
    const organizationName = event.request.userAttributes['custom:organization_name'] || 'My Organization';
    if (!tenantId) return event;
    event.response.claimsAndScopeOverrideDetails = {
      accessTokenGeneration: { claimsToAddOrOverride: {
        tenant_id: tenantId,
        organization_name: organizationName,
        email: event.request.userAttributes.email,
      }},
      idTokenGeneration: { claimsToAddOrOverride: {
        tenant_id: tenantId,
        organization_name: organizationName,
      }},
    };
  }
  return event;
};
