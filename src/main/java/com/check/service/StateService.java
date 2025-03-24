package com.check.service;

import org.springframework.stereotype.Service;
import java.io.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class StateService {
    private static final String STATE_FILE = "app-state.dat";
    private Map<String, Object> state = new ConcurrentHashMap<>();

    public void saveState(String key, Object value) {
        state.put(key, value);
        persistState();
    }

    public Object getState(String key) {
        return state.get(key);
    }

    private void persistState() {
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new FileOutputStream(STATE_FILE))) {
            oos.writeObject(state);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadState() {
        File file = new File(STATE_FILE);
        if (file.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(
                    new FileInputStream(file))) {
                state = (Map<String, Object>) ois.readObject();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
} 