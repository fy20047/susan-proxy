package com.fy20047.susan.service;

import com.fy20047.susan.domain.OrderGroup;
import com.fy20047.susan.repository.OrderGroupRepository;
import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SheetSyncWriter {

    private final OrderGroupRepository orderGroupRepository;

    public SheetSyncWriter(OrderGroupRepository orderGroupRepository) {
        this.orderGroupRepository = orderGroupRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void replaceGroups(String groupName, Collection<OrderGroup> groups) {
        List<OrderGroup> existingGroups = orderGroupRepository.findByGroupName(groupName);
        if (!existingGroups.isEmpty()) {
            orderGroupRepository.deleteAll(existingGroups);
        }
        orderGroupRepository.saveAll(groups);
    }
}
