package com.fy20047.susan.service;

import com.fy20047.susan.domain.OrderGroup;
import com.fy20047.susan.domain.OrderItem;
import com.fy20047.susan.repository.OrderGroupRepository;
import com.fy20047.susan.repository.OrderItemRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SheetSyncWriter {

    private static final Logger log = LoggerFactory.getLogger(SheetSyncWriter.class);

    private final OrderGroupRepository orderGroupRepository;
    private final OrderItemRepository orderItemRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public SheetSyncWriter(OrderGroupRepository orderGroupRepository, OrderItemRepository orderItemRepository) {
        this.orderGroupRepository = orderGroupRepository;
        this.orderItemRepository = orderItemRepository;
    }

    @Transactional
    public void prepareReplace(String groupName, String sourceKey) {
        List<OrderGroup> existingGroups = findExistingGroups(groupName, sourceKey);
        if (!existingGroups.isEmpty()) {
            orderGroupRepository.deleteAll(existingGroups);
        }
    }

    @Transactional
    public Long createGroup(OrderGroup group) {
        OrderGroup saved = orderGroupRepository.save(group);
        entityManager.flush();
        entityManager.clear();
        return saved.getId();
    }

    @Transactional
    public void saveItems(List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        attachGroupReferences(items);
        orderItemRepository.saveAll(items);
        entityManager.flush();
        entityManager.clear();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveItem(OrderItem item) {
        if (item == null) {
            return;
        }
        attachGroupReference(item);
        orderItemRepository.save(item);
        entityManager.flush();
        entityManager.clear();
    }

    @Transactional
    public void updateGroupBonuses(Map<Long, Integer> bonusByGroupId) {
        if (bonusByGroupId == null || bonusByGroupId.isEmpty()) {
            return;
        }
        for (Map.Entry<Long, Integer> entry : bonusByGroupId.entrySet()) {
            Integer bonus = entry.getValue();
            if (bonus == null) {
                continue;
            }
            OrderGroup group = entityManager.getReference(OrderGroup.class, entry.getKey());
            group.setBonusCount(bonus);
        }
        entityManager.flush();
        entityManager.clear();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void replaceGroups(String groupName, String sourceKey, Collection<OrderGroup> groups) {
        List<OrderGroup> existingGroups = findExistingGroups(groupName, sourceKey);
        orderGroupRepository.saveAll(groups);
        if (!existingGroups.isEmpty()) {
            orderGroupRepository.deleteAll(existingGroups);
        }
    }

    private List<OrderGroup> findExistingGroups(String groupName, String sourceKey) {
        if (sourceKey == null || sourceKey.isBlank()) {
            return orderGroupRepository.findByGroupName(groupName);
        }
        return orderGroupRepository.findByGroupNameAndSourceKeyIncludingLegacy(groupName, sourceKey);
    }

    private void attachGroupReferences(List<OrderItem> items) {
        for (OrderItem item : items) {
            attachGroupReference(item);
        }
    }

    private void attachGroupReference(OrderItem item) {
        if (item == null) {
            return;
        }
        OrderGroup group = item.getOrderGroup();
        if (group == null || group.getId() == null) {
            log.warn("OrderItem missing orderGroup reference before save.");
            return;
        }
        item.setOrderGroup(entityManager.getReference(OrderGroup.class, group.getId()));
    }
}
