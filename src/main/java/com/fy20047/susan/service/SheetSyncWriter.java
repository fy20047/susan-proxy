package com.fy20047.susan.service;

import com.fy20047.susan.domain.OrderGroup;
import com.fy20047.susan.repository.OrderGroupRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SheetSyncWriter {

    private final OrderGroupRepository orderGroupRepository;
    @PersistenceContext
    private EntityManager entityManager;

    public SheetSyncWriter(OrderGroupRepository orderGroupRepository) {
        this.orderGroupRepository = orderGroupRepository;
    }

    @Transactional
    public void prepareReplace(String groupName) {
        List<OrderGroup> existingGroups = orderGroupRepository.findByGroupName(groupName);
        if (!existingGroups.isEmpty()) {
            orderGroupRepository.deleteAll(existingGroups);
        }
    }

    @Transactional
    public void saveGroup(OrderGroup group) {
        orderGroupRepository.save(group);
        entityManager.flush();
        entityManager.clear();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void replaceGroups(String groupName, Collection<OrderGroup> groups) {
        List<OrderGroup> existingGroups = orderGroupRepository.findByGroupName(groupName);
        orderGroupRepository.saveAll(groups);
        if (!existingGroups.isEmpty()) {
            orderGroupRepository.deleteAll(existingGroups);
        }
    }
}
