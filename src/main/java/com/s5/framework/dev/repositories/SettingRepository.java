package com.s5.framework.dev.repositories;

import com.s5.framework.dev.models.Setting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SettingRepository extends JpaRepository<Setting, Long> {
    
    Optional<Setting> findByKey(String key);
}
