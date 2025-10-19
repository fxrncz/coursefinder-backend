package com.app.config;

import com.app.models.User;
import com.app.repositories.UserRepository;
import com.app.security.PasswordUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Value("${seed.enabled:false}")
    private boolean seedEnabled;

    @Override
    public void run(String... args) throws Exception {
        if (!seedEnabled) {
            return;
        }

        long count = userRepository.count();
        if (count > 0) {
            return;
        }

        User alice = new User("alice", "alice@example.com", PasswordUtil.hashPassword("password123"));
        alice.setAge(22);
        alice.setGender("Female");

        User bob = new User("bob", "bob@example.com", PasswordUtil.hashPassword("password123"));
        bob.setAge(25);
        bob.setGender("Male");

        User carol = new User("carol", "carol@example.com", PasswordUtil.hashPassword("password123"));
        carol.setAge(28);
        carol.setGender("Other");

        userRepository.saveAll(List.of(alice, bob, carol));
        System.out.println("Seeded sample users: alice, bob, carol");
    }
}


