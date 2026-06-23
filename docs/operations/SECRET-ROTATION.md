# Secret rotation runbook

Use this checklist when rotating credentials or after a suspected leak. Record the date and operator
in [DRILL-LOG.md](../saas/DRILL-LOG.md) when a rotation drill completes.

## Stripe

1. Create a new webhook signing secret in the Stripe Dashboard for the production endpoint.
2. Update the secret in AWS Secrets Manager (`StripeWebhookSecretArn` in CloudFormation).
3. Redeploy App Runner so the service reads the new value.
4. Send a test event from Stripe and confirm `200` plus plan sync in logs.
5. Revoke the previous webhook secret after the new one is verified.

## Database (RDS)

1. Create a new password in Secrets Manager or rotate through RDS.
2. Update the App Runner environment / secret reference.
3. Redeploy and confirm `/actuator/health` reports `UP`.
4. Run `./scripts/drills/verify-rds-backups.sh` after rotation.

## Cognito app client

1. Create a new app client or rotate the client secret if using a confidential client.
2. Update Vercel public runtime config (`clientId`, domain) and redeploy the frontend.
3. Verify sign-in and Start Free onboarding with a test account.

## Quarterly review

- Confirm no secrets appear in git history, CI logs, or browser bundles.
- Review IAM roles attached to App Runner and RDS.
- Confirm Stripe, Cognito, and database passwords are not shared across environments.
