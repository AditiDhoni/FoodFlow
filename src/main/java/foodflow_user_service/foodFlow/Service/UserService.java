package foodflow_user_service.foodFlow.Service;

import foodflow_user_service.foodFlow.DTO.LoginRequest;
import foodflow_user_service.foodFlow.DTO.LoginResponse;
import foodflow_user_service.foodFlow.DTO.RegisterRequest;
import foodflow_user_service.foodFlow.DTO.UserResponse;
import foodflow_user_service.foodFlow.Entity.User;
import foodflow_user_service.foodFlow.Exception.DuplicateResourceException;
import foodflow_user_service.foodFlow.Exception.ResourceNotFoundException;
import foodflow_user_service.foodFlow.Repository.UserRepository;
import foodflow_user_service.foodFlow.Security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public UserResponse register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException(
                    "User with this email already exists"
            );
        }

        String encodedPassword =
                passwordEncoder.encode(request.getPassword());

        User user = new User(
                request.getName(),
                request.getEmail(),
                encodedPassword,
                "CUSTOMER"
        );

        User savedUser = userRepository.save(user);

        return convertToResponse(savedUser);
    }

    public LoginResponse login(LoginRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found"
                        )
                );

        boolean passwordMatches =
                passwordEncoder.matches(
                        request.getPassword(),
                        user.getPassword()
                );

        if (!passwordMatches) {
            throw new IllegalArgumentException(
                    "Invalid email or password"
            );
        }

        String token =
                jwtService.generateToken(
                        user.getEmail(),
                        user.getRole()
                );

        UserResponse userResponse =
                convertToResponse(user);

        return new LoginResponse(token, userResponse);
    }

    public UserResponse getUserById(Long id) {

        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found with id: " + id
                        )
                );

        return convertToResponse(user);
    }

    private UserResponse convertToResponse(User user) {

        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole()
        );
    }
}