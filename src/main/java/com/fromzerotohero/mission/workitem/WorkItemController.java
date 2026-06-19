package com.fromzerotohero.mission.workitem;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/work-items")
public class WorkItemController {
    private final WorkItemService service;
    private final WorkItemModelAssembler assembler;

    public WorkItemController(WorkItemService service, WorkItemModelAssembler assembler) {
        this.service = service;
        this.assembler = assembler;
    }

    @GetMapping
    public CollectionModel<EntityModel<WorkItem>> all() {
        List<EntityModel<WorkItem>> items = service.findAll()
                .stream().map(assembler::toModel).toList();
        return CollectionModel.of(items,
                linkTo(methodOn(WorkItemController.class).all()).withSelfRel());
    }

    @GetMapping("/{id}")
    public EntityModel<WorkItem> one(@PathVariable Long id) {
        return assembler.toModel(service.find(id));
    }

    @PostMapping
    public ResponseEntity<EntityModel<WorkItem>> create(@Valid @RequestBody WorkItemRequest request) {
        WorkItem saved = service.create(request);
        EntityModel<WorkItem> model = assembler.toModel(saved);
        URI location = model.getRequiredLink("self").toUri();
        return ResponseEntity.created(location).body(model);
    }

    @PutMapping("/{id}")
    public EntityModel<WorkItem> replace(@PathVariable Long id,
                                         @Valid @RequestBody WorkItemRequest request) {
        return assembler.toModel(service.replace(id, request));
    }

    @PatchMapping("/{id}/status")
    public EntityModel<WorkItem> changeStatus(@PathVariable Long id,
            @Valid @RequestBody WorkItemStatusRequest request) {
        return assembler.toModel(service.changeStatus(id, request.status()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
