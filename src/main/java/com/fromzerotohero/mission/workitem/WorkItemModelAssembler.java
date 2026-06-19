package com.fromzerotohero.mission.workitem;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

@Component
public class WorkItemModelAssembler implements RepresentationModelAssembler<WorkItem, EntityModel<WorkItem>> {
    @Override
    public EntityModel<WorkItem> toModel(WorkItem item) {
        return EntityModel.of(item,
                linkTo(methodOn(WorkItemController.class).one(item.getId())).withSelfRel(),
                linkTo(methodOn(WorkItemController.class).changeStatus(item.getId(), null)).withRel("status"),
                linkTo(methodOn(WorkItemController.class).all()).withRel("workItems"));
    }
}
