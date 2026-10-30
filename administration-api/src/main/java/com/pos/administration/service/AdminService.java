package com.pos.administration.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pos.administration.domain.Setting;
import com.pos.administration.domain.Store;
import com.pos.administration.domain.Terminal;
import com.pos.administration.mapper.SettingMapper;
import com.pos.administration.mapper.StoreMapper;
import com.pos.administration.mapper.TerminalMapper;
import com.pos.administration.web.AdminDtos.SettingRequest;
import com.pos.administration.web.AdminDtos.StoreRequest;
import com.pos.administration.web.AdminDtos.TerminalRequest;
import com.pos.common.error.ApiException;
import com.pos.common.security.CurrentUser;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminService {

    private final StoreMapper stores;
    private final TerminalMapper terminals;
    private final SettingMapper settings;

    // ---- stores
    @Transactional(readOnly = true)
    public List<Store> listStores() {
        return stores.findAll();
    }

    @Transactional(readOnly = true)
    public Store getStore(Long id) {
        Store store = stores.findById(id);
        if (store == null) {
            throw ApiException.notFound("Store", id);
        }
        return store;
    }

    public Store createStore(StoreRequest request) {
        Store store = new Store();
        apply(store, request);
        stores.insert(store);
        return getStore(store.getId());
    }

    public Store updateStore(Long id, StoreRequest request) {
        Store store = getStore(id);
        apply(store, request);
        stores.update(store);
        return getStore(id);
    }

    public Store setStoreActive(Long id, boolean active) {
        getStore(id);
        stores.setActive(id, active);
        return getStore(id);
    }

    private void apply(Store store, StoreRequest request) {
        store.setCode(request.code().trim().toUpperCase());
        store.setName(request.name().trim());
        store.setAddress(request.address());
        store.setPhone(request.phone());
        store.setTin(request.tin());
    }

    // ---- terminals
    @Transactional(readOnly = true)
    public List<Terminal> listTerminals(Long storeId) {
        return terminals.findAll(CurrentUser.effectiveStoreId(storeId));
    }

    @Transactional(readOnly = true)
    public Terminal getTerminal(Long id) {
        Terminal terminal = terminals.findById(id);
        if (terminal == null) {
            throw ApiException.notFound("Terminal", id);
        }
        return terminal;
    }

    public Terminal createTerminal(TerminalRequest request) {
        getStore(request.storeId());
        Terminal terminal = new Terminal();
        terminal.setStoreId(request.storeId());
        terminal.setCode(request.code().trim().toUpperCase());
        terminal.setName(request.name().trim());
        terminals.insert(terminal);
        return getTerminal(terminal.getId());
    }

    public Terminal updateTerminal(Long id, TerminalRequest request) {
        Terminal terminal = getTerminal(id);
        getStore(request.storeId());
        terminal.setStoreId(request.storeId());
        terminal.setCode(request.code().trim().toUpperCase());
        terminal.setName(request.name().trim());
        terminals.update(terminal);
        return getTerminal(id);
    }

    public Terminal setTerminalActive(Long id, boolean active) {
        getTerminal(id);
        terminals.setActive(id, active);
        return getTerminal(id);
    }

    public void heartbeat(Long id) {
        Terminal terminal = getTerminal(id);
        CurrentUser.requireStoreAccess(terminal.getStoreId());
        terminals.heartbeat(id);
    }

    // ---- settings
    /** Effective settings of a store: global defaults overlaid with the store's own values. */
    @Transactional(readOnly = true)
    public Map<String, String> effectiveSettings(Long storeId) {
        Long store = storeId == null ? 0L : storeId;
        Map<String, String> result = new LinkedHashMap<>();
        for (Setting setting : settings.findEffective(store)) {
            result.put(setting.getSettingKey(), setting.getSettingValue());
        }
        return result;
    }

    public Map<String, String> saveSetting(SettingRequest request) {
        Long store = request.storeId() == null ? 0L : request.storeId();
        if (store != 0L) {
            getStore(store);
        }
        settings.upsert(store, request.key().trim(), request.value());
        return effectiveSettings(store);
    }
}
