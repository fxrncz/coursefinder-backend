package com.app.config;

import com.app.models.MbtiRiasecMapping;
import com.app.repositories.MbtiRiasecMappingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class MbtiDataSeeder implements CommandLineRunner {

    @Autowired
    private MbtiRiasecMappingRepository mappingRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${seed.mbti.enabled:true}")
    private boolean seedEnabled;

    @Override
    public void run(String... args) throws Exception {
        if (!seedEnabled) {
            System.out.println("MBTI data seeding is disabled");
            return;
        }

        // Check if data already exists
        long existingCount = mappingRepository.countTotalMappings();
        if (existingCount > 0) {
            System.out.println("MBTI data already exists (" + existingCount + " records). Skipping seed.");
            return;
        }

        System.out.println("Seeding MBTI+RIASEC matching data...");
        
        try {
            // Load and execute the SQL file for the new matching table
            ClassPathResource resource = new ClassPathResource("data/mbti_riasec_matching.sql");
            byte[] data = FileCopyUtils.copyToByteArray(resource.getInputStream());
            String sql = new String(data, StandardCharsets.UTF_8);
            
            // Execute the SQL
            jdbcTemplate.execute(sql);
            
            long newCount = mappingRepository.countTotalMappings();
            System.out.println("Successfully seeded " + newCount + " matching records");
            
        } catch (IOException e) {
            System.err.println("Failed to load matching data file: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Failed to seed matching data: " + e.getMessage());
        }
    }
}
