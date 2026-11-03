package com.pos.administration.service;

import java.util.List;
import java.util.Map;

import com.pos.administration.dto.Store;
import com.pos.administration.dto.Terminal;
import com.pos.administration.dto.AdminDtos.SettingRequest;
import com.pos.administration.dto.AdminDtos.StoreRequest;
import com.pos.administration.dto.AdminDtos.TerminalRequest;

public interface AdminService {

    List<Store> listStores();

    Store getStore(Long id);

    Store createStore(StoreRequest request);

    Store updateStore(Long id, StoreRequest request);

    Store setStoreActive(Long id, boolean active);

    List<Terminal> listTerminals(Long storeId);

    Terminal getTerminal(Long id);

    Terminal createTerminal(TerminalRequest request);

    Terminal updateTerminal(Long id, TerminalRequest request);

    Terminal setTerminalActive(Long id, boolean active);

    void heartbeat(Long id);

    Map<String, String> effectiveSettings(Long storeId);

    Map<String, String> saveSetting(SettingRequest request);
}
