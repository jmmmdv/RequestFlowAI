package com.fromzerotohero.mission.workitem;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fromzerotohero.mission.security.TenantContext;

@Service
public class WorkItemService {
    private final WorkItemRepository repository;
    private final TenantContext tenantContext;

    public WorkItemService(WorkItemRepository repository, TenantContext tenantContext) {
        this.repository = repository;
        this.tenantContext = tenantContext;
    }

    @Transactional(readOnly = true)
    public List<WorkItem> findAll() {
        return repository.findAllByTenantIdOrderByUpdatedAtDesc(tenantContext.tenantId());
    }

    @Transactional(readOnly = true)
    public WorkItem find(Long id) {
        return repository.findByIdAndTenantId(id, tenantContext.tenantId())
                .orElseThrow(() -> new WorkItemNotFoundException(id));
    }

    @Transactional
    public WorkItem create(WorkItemRequest request) {
        return repository.save(new WorkItem(normalize(request.title()), request.description(),
                request.priority(), request.status(), tenantContext.tenantId()));
    }

    @Transactional
    public WorkItem replace(Long id, WorkItemRequest request) {
        WorkItem item = find(id);
        item.setTitle(normalize(request.title()));
        item.setDescription(request.description());
        item.setPriority(request.priority());
        item.setStatus(request.status());
        return item;
    }

    @Transactional
    public WorkItem changeStatus(Long id, WorkStatus status) {
        WorkItem item = find(id);
        item.setStatus(status);
        return item;
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(find(id));
    }

    private String normalize(String value) {
        return value.trim().replaceAll("\\s+", " ");
    }
}
