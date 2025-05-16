package com.pm.patient_service.mapper;

import com.pm.patient_service.dto.PatientRequestDTO;
import com.pm.patient_service.dto.PatientResponseDTO;
import com.pm.patient_service.model.Patient;

import java.time.LocalDate;

public class PatientMapper {

    public static PatientResponseDTO toDTO(Patient patient) {
        PatientResponseDTO patientdto = new PatientResponseDTO();
        patientdto.setId(patient.getId().toString());
        patientdto.setName(patient.getName());
        patientdto.setEmail(patient.getEmail());
        patientdto.setAddress(patient.getAddress());
        patientdto.setDateOfBirth(patient.getDateOfBirth().toString());
        return patientdto;
    }

    public static Patient toModel(PatientRequestDTO patientRequestDTO) {
        Patient patient = new Patient();
        patient.setName(patientRequestDTO.getName());
        patient.setEmail(patientRequestDTO.getEmail());
        patient.setAddress(patientRequestDTO.getAddress());
        patient.setDateOfBirth(LocalDate.parse(patientRequestDTO.getDateOfBirth()));
        patient.setRegisteredDate(LocalDate.parse(patientRequestDTO.getRegisteredDate()));
        return patient;
    }
}
