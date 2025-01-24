package com.example.demo.controllers;

import com.example.demo.DTOs.LoginDTO;
import com.example.demo.DTOs.UserDTO;
import com.example.demo.models.Post;
import com.example.demo.models.User;
import com.example.demo.services.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService){
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody @Valid UserDTO userDTO){
        User user = userService.registerUser(userDTO);
        if (user != null) {
            return ResponseEntity.ok(user);
        } else {
            return ResponseEntity.status(409).body("Пользователь уже зарегистрирован");
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginDTO loginDTO, HttpSession session){
        User user = userService.authenticate(loginDTO.getEmail(), loginDTO.getPassword());
        if (user != null){
            session.setAttribute("userId",user.getId());
            userService.incrementVisitCount(user.getId());
            return ResponseEntity.ok("Добро пожаловать");
        }
        else{
            return ResponseEntity.status(401).body("Неверное имя пользователя или пароль");
        }
    }

    @GetMapping("/getInfo")
    public ResponseEntity<?> getUserInfo(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).body("Пользователь не авторизован");
        }
        User user = userService.getUserById(userId);
        if (user != null) {
            return ResponseEntity.ok(user);
        } else {
            return ResponseEntity.status(404).body("Пользователь не найден");
        }
    }

    @GetMapping("/getLiked")
    public ResponseEntity<?> getUserLiked(HttpSession session){
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).body("Пользователь не авторизован");
        }
        List<Post> likedPost = userService.getUserById(userId).getLiked_post();
        if (likedPost != null) {
            return ResponseEntity.ok(likedPost);
        } else {
            return ResponseEntity.status(404).body("Пользователь не найден");
        }
    }

    @PutMapping("/dashboard/edit")
    public ResponseEntity<?> uploadProfile(HttpSession session,
                                               @RequestParam(required=false) String name,
                                               @RequestParam(required=false) String password,
                                               @RequestParam(required=false) MultipartFile avatar){
        try{
            Long userId = (Long) session.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(401).body("Пользователь не авторизован");
            }
            byte[] avatarBytes = null;
            if (avatar != null) {
                avatarBytes = avatar.getBytes();
            }
            userService.updateProfile(userId, password, name, avatarBytes);
            User user = userService.getUserById(userId);
            return ResponseEntity.ok(user);
        }
        catch(Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @PutMapping("/dashboard/setDefaultAvatar")
    public ResponseEntity<?> setDefaultAvatar(HttpSession session){
        try{
            Long userId = (Long) session.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(401).body("Пользователь не авторизован");
            }
            userService.deleteAvatar(userId);
            User user = userService.getUserById(userId);
            return ResponseEntity.ok(user);
        }
        catch(Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @PutMapping("/viewNews/{id}/likeNew")
    public ResponseEntity<?> likeNew(HttpSession session,
                                     @PathVariable Long id){
        try{
            Long userId = (Long) session.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(401).body("Пользователь не авторизован");
            }
            userService.addNewToProfile(userId, id);
            User user = userService.getUserById(userId);
            return ResponseEntity.ok(user);
        }
        catch(Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @DeleteMapping("/viewNews/{id}/delete")
    public ResponseEntity<?> deleteNews(@PathVariable Long id){
        userService.deletePostById(id);
        return ResponseEntity.ok("Новость успешно удалена из избранного");
    }

    @PostMapping("/dashboard/logout")
    public ResponseEntity<?> logout(HttpSession session, HttpServletResponse response, HttpServletRequest request) {
        session.invalidate();
        Cookie[] cookies = request.getCookies();

        // Ищем куку с именем "JSESSIONID"
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                System.out.println(cookie);
                if ("JSESSIONID".equals(cookie.getName())) {
                    cookie.setMaxAge(0);
                    cookie.setPath("/");
                    cookie.setSecure(true);
                    cookie.setHttpOnly(true);
                    response.addCookie(cookie);
                    break;
                }
            }
        }
        return ResponseEntity.ok("Вы успешно вышли из системы" + session);
    }
}
