package com.cliniccare.interfaces;

import com.cliniccare.exceptions.ClinicCareException;
import java.util.List;

/** Common CRUD operations for every DAO. */
public interface Repository<T> {
    boolean save(T entity) throws ClinicCareException;
    T findById(int id) throws ClinicCareException;
    List<T> findAll() throws ClinicCareException;
    boolean update(T entity) throws ClinicCareException;
    boolean delete(int id) throws ClinicCareException;
}
