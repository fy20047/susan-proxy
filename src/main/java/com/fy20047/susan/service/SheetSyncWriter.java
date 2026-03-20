package com.fy20047.susan.service;

import com.fy20047.susan.domain.OrderGroup;
import com.fy20047.susan.domain.OrderItem;
import com.fy20047.susan.repository.OrderItemRepository;
import com.fy20047.susan.repository.OrderGroupRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SheetSyncWriter {

    private final OrderGroupRepository orderGroupRepository;
    private final OrderItemRepository orderItemRepository;
    @PersistenceContext
    private EntityManager entityManager;

    public SheetSyncWriter(OrderGroupRepository orderGroupRepository, OrderItemRepository orderItemRepository) {
        this.orderGroupRepository = orderGroupRepository;
        this.orderItemRepository = orderItemRepository;
    }

    @Transactional
    public void prepareReplace(String groupName) {
        List<OrderGroup> existingGroups = orderGroupRepository.findByGroupName(groupName);
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
        orderItemRepository.saveAll(items);
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
    public void replaceGroups(String groupName, Collection<OrderGroup> groups) {
        List<OrderGroup> existingGroups = orderGroupRepository.findByGroupName(groupName);
        orderGroupRepository.saveAll(groups);
        if (!existingGroups.isEmpty()) {
            orderGroupRepository.deleteAll(existingGroups);
        }
    }
}
