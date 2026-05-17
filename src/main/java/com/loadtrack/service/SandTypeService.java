package com.loadtrack.service;

import com.loadtrack.dto.SandTypeRequest;
import com.loadtrack.entity.SandType;
import com.loadtrack.exception.DuplicateEntryException;
import com.loadtrack.exception.ResourceNotFoundException;
import com.loadtrack.repository.SandTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SandTypeService {

    @Autowired
    private SandTypeRepository sandTypeRepository;

    public List<SandType> getAllSandTypes() {
        return sandTypeRepository.findAll();
    }

    public SandType getSandTypeById(Integer id) {
        return sandTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sand type not found with id: " + id));
    }

    public SandType addSandType(SandTypeRequest request) {
        if (sandTypeRepository.existsByName(request.getName())) {
            throw new DuplicateEntryException("Sand type already exists: " + request.getName());
        }
        SandType sandType = new SandType();
        sandType.setName(request.getName());
        sandType.setPricePerTon(request.getPricePerTon());
        return sandTypeRepository.save(sandType);
    }

    public SandType updateSandType(Integer id, SandTypeRequest request) {
        SandType sandType = getSandTypeById(id);
        sandType.setName(request.getName());
        sandType.setPricePerTon(request.getPricePerTon());
        return sandTypeRepository.save(sandType);
    }

    public void deleteSandType(Integer id) {
        SandType sandType = getSandTypeById(id);
        sandTypeRepository.delete(sandType);
    }
}
