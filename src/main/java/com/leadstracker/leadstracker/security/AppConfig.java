package com.leadstracker.leadstracker.security;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import com.leadstracker.leadstracker.DTO.ClientDto;
import com.leadstracker.leadstracker.DTO.TeamDto;
import com.leadstracker.leadstracker.DTO.UserDto;
import com.leadstracker.leadstracker.entities.ClientEntity;
import com.leadstracker.leadstracker.entities.UserEntity;
import com.leadstracker.leadstracker.request.ClientDetails;
import com.leadstracker.leadstracker.request.TeamDetails;
import com.leadstracker.leadstracker.request.UserDetails;
import com.leadstracker.leadstracker.response.ClientRest;
import com.leadstracker.leadstracker.response.Statuses;
import jakarta.annotation.PostConstruct;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.modelmapper.convention.NameTokenizers;
import org.modelmapper.spi.MappingContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Configuration
public class AppConfig {

    @Value("${AWS_SECRET_KEY:}")
    private String awsSecretKey;

    @Value("${AWS_ACCESS_KEY:}")
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
                    map.map(src -> src.getTeam().getId().toString(), UserDto::setTeamName);
                    map.map(src -> src.getTeamLead().getUserId(), UserDto::setTeamLeadUserId);
                });

        mapper.typeMap(TeamDetails.class, TeamDto.class).addMappings(m -> {
            m.map(TeamDetails::getTeamLeadUserId, TeamDto::setTeamLeadId);
        });


        mapper.typeMap(ClientDetails.class, ClientDto.class)
                .addMappings(m -> {
                    m.map(ClientDetails::getClientStatus, ClientDto::setClientStatus);
                    m.map(ClientDetails::getGpsLocation, ClientDto::setGpsLocation);
                });


        Converter<Date, LocalDateTime> dateToLocalDateTimeConverter = ctx -> {
            Date source = ctx.getSource();
            if (source == null) return null;
            return LocalDateTime.ofInstant(source.toInstant(), ZoneId.systemDefault());
        };

        mapper.typeMap(ClientDto.class, ClientRest.class).addMappings(m -> {
            m.map(ClientDto::getClientId, ClientRest::setClientId);
            m.map(ClientDto::getFirstName, ClientRest::setFirstName);
            m.map(ClientDto::getLastName, ClientRest::setLastName);
            m.map(ClientDto::getPhoneNumber, ClientRest::setPhoneNumber);
            m.<String>map(src -> {
                if (src.getClientStatus() == null) return null;
                try {
                    Statuses statusEnum = Statuses.fromString(src.getClientStatus());
                    return statusEnum.getDisplayName();
                } catch (Exception e) {
                    return src.getClientStatus();
                }
            }, ClientRest::setClientStatus);

            // Use explicit converter
            m.using(dateToLocalDateTimeConverter).map(ClientDto::getCreatedDate, ClientRest::setCreatedAt);
            m.using(dateToLocalDateTimeConverter).map(ClientDto::getLastUpdated, ClientRest::setLastUpdated);

            m.map(ClientDto::getGpsLocation, ClientRest::setGpsLocation);
        });



        mapper.addConverter(new Converter<Date, LocalDateTime>() {
            public LocalDateTime convert(MappingContext<Date, LocalDateTime> ctx) {
                Date source = ctx.getSource();
                if (source == null) return null;
                return LocalDateTime.ofInstant(source.toInstant(), ZoneId.systemDefault());
            }
        });

        mapper.addConverter(new Converter<Date, LocalDate>() {
            public LocalDate convert(MappingContext<Date, LocalDate> ctx) {
                Date source = ctx.getSource();
                if (source == null) return null;
                return source.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            }
        });

        mapper.addConverter(new Converter<LocalDateTime, Date>() {
            public Date convert(MappingContext<LocalDateTime, Date> ctx) {
                LocalDateTime source = ctx.getSource();
                if (source == null) return null;
                return Date.from(source.atZone(ZoneId.systemDefault()).toInstant());
            }
        });



//        mapper.getConfiguration()
//                .setSourceNameTokenizer(NameTokenizers.UNDERSCORE)
//                .setDestinationNameTokenizer(NameTokenizers.CAMEL_CASE)
//                .setFieldMatchingEnabled(true)
//                .setMatchingStrategy(MatchingStrategies.STRICT);

        mapper.getConfiguration()
                .setSourceNameTokenizer(NameTokenizers.CAMEL_CASE)
                .setDestinationNameTokenizer(NameTokenizers.CAMEL_CASE)
                .setFieldMatchingEnabled(true)
                .setMatchingStrategy(MatchingStrategies.STRICT);

        return mapper;
    }


    @Bean
    public String secretKey( @Value("${token.secret:}") String key) {
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
