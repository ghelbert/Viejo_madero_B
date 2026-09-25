package com.example.mi_api.restaurant.service;

import com.example.mi_api.restaurant.repository.TableRepository;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class TableService {

    private final TableRepository tableRepository;

    public TableService(TableRepository tableRepository) {
        this.tableRepository = tableRepository;
    }

    public List<Map<String, Object>> findAll() {
        return tableRepository.findAll();
    }

    public Map<String, Object> findDetail(long id) {
        return tableRepository.findDetail(id);
    }
}
