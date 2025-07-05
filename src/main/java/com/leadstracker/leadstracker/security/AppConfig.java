package com.leadstracker.leadstracker.security;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import com.leadstracker.leadstracker.DTO.UserDto;
import com.leadstracker.leadstracker.entities.UserEntity;
import com.leadstracker.leadstracker.request.UserDetails;
import jakarta.annotation.PostConstruct;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
public class AppConfig {

    @Value("${aws.secretKey}")
    private String awsSecretKey;

    @Value("${aws.accessKey}")
    private String awsAccessKey;

    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

//    @Bean
//    public ModelMapper modelMapper() {
//        return new ModelMapper();
//    }

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper mapper = new ModelMapper();
        mapper.getConfiguration()
                .setMatchingStrategy(MatchingStrategies.STRICT)
                .setAmbiguityIgnored(true);

        // UserDetails -> UserDto mapping
        mapper.typeMap(UserDetails.class, UserDto.class)
                .addMappings(map -> {
                    map.map(UserDetails::getTeamLeadUserId, UserDto::setTeamLeadUserId);
                    map.map(UserDetails::getRole, UserDto::setRole);
                    map.map(UserDetails::getStaffId, UserDto::setStaffId);
                });

        // UserEntity -> UserDto mapping
        mapper.typeMap(UserEntity.class, UserDto.class)
                .addMappings(map -> {
                    map.map(src -> src.getRole().getName(), UserDto::setRole);
                });

        return mapper;
    }


    @Bean
    public String secretKey( @Value("${token.secret}") String key) {
        return key;
    }

    @Bean
    public AmazonSimpleEmailService awsSimpleEmailService () {
        return AmazonSimpleEmailServiceClientBuilder
                .standard()
                .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(awsAccessKey, awsSecretKey)))
                .withRegion(Regions.US_EAST_1)
                .build();
    }
    
}
