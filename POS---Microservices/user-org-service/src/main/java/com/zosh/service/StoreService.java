package com.zosh.service;

import com.zosh.domain.StoreStatus;
import com.zosh.exception.ResourceNotFoundException;
import com.zosh.exception.UserException;
import com.zosh.modal.Store;
import com.zosh.modal.User;
import com.zosh.payload.dto.StoreDTO;
import com.zosh.payload.dto.StoreDashboardStatsDTO;
import com.zosh.payload.dto.StoreRegistrationStatDTO;
import com.zosh.payload.dto.UserDTO;

import java.util.List;

public interface StoreService {
    StoreDTO createStore(StoreDTO storeDto, User user) throws UserException;
    StoreDTO getStoreById(Long id) throws ResourceNotFoundException, UserException;
    List<StoreDTO> getAllStores(StoreStatus status);
    StoreDashboardStatsDTO getDashboardStats();
    List<StoreRegistrationStatDTO> getLast7DayRegistrationStats();
    Store getStoreByAdminId() throws UserException;
    StoreDTO getStoreByEmployee() throws UserException;
    boolean currentUserCanAccessStore(Long storeId) throws UserException;
    StoreDTO updateStore(Long id, StoreDTO storeDto) throws ResourceNotFoundException, UserException;
    void deleteStore() throws ResourceNotFoundException, UserException;
    UserDTO addEmployee(Long id, UserDTO userDto) throws UserException;
    List<UserDTO> getEmployeesByStore(Long storeId) throws UserException;
    StoreDTO moderateStore(Long storeId, StoreStatus action) throws ResourceNotFoundException;
}
