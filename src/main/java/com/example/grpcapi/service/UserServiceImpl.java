package com.example.grpcapi.service;

import com.example.grpcapi.UserServiceGrpc;
import com.example.grpcapi.RegisterRequest;
import com.example.grpcapi.RegisterResponse;
import com.example.grpcapi.entity.User;
import com.example.grpcapi.entity.UserInfo;
import com.example.grpcapi.repository.UserRepository;
import com.example.grpcapi.repository.UserInfoRepository;
import com.example.grpcapi.util.Hash;
import com.example.grpcapi.util.SnowflakeIdGenerator;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.UUID;

@GrpcService
public class UserServiceImpl extends UserServiceGrpc.UserServiceImplBase {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserInfoRepository userInfoRepository;

    private SnowflakeIdGenerator idGenerator = new SnowflakeIdGenerator(1, 1);

    @Override
    public void registerUser(RegisterRequest request, StreamObserver<RegisterResponse> responseObserver) {
        if (userRepository.existsByUsername(request.getUsername())) {
            responseObserver.onError(
                    Status.ALREADY_EXISTS.withDescription("Username already exists").asRuntimeException()
            );
            return;
        }

        // Generate ID and hash password
        Long userId = idGenerator.generateId();
        long saltI = idGenerator.generateId();
        String salt = Long.toString(saltI);
        String hashedPassword = Hash.hashPassword(request.getPassword(), salt);

        // Save to User table
        User user = new User();
        user.setId(userId);
        user.setFullname(request.getFullname());
        user.setUsername(request.getUsername());
        user.setPassword(hashedPassword);
        user.setPasswordSalt(salt);
        user.setCreatedAt(LocalDateTime.now());
        userRepository.save(user);

        // Save to UserInfo table
        UserInfo userInfo = new UserInfo();
        userInfo.setUserId(userId);
        userInfo.setAge(request.getAge());
        userInfo.setAddress(request.getAddress());
        userInfoRepository.save(userInfo);

        // Response
        RegisterResponse response = RegisterResponse.newBuilder()
                .setMessage("User registered successfully")
                .setUserId(userId)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
