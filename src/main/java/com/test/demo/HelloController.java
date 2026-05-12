package com.test.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/users")
public class HelloController {

    @Autowired
    private UserRepository userRepository;
    
    @GetMapping("hello")
    public String hello() {
        return "hello, world!";
    }

     @PostMapping
    public User createUser(@RequestBody User user) {
        return userRepository.save(user);
    }
    
    @GetMapping("/{id}")
    public User getUser(@PathVariable Long id) {
        return userRepository.findById(id).orElse(null);
    }
    
    @GetMapping("/all")
    public java.util.List<User> getAllUsers() {
        return userRepository.findAll();
    }
    
    @GetMapping("/email/{email}")
    public User getUserByEmail(@PathVariable String email) {
        return userRepository.findByEmail(email);
    }
    
    @DeleteMapping("/{id}")
    public void deleteUser(@PathVariable Long id) {
        userRepository.deleteById(id);
    }
}
