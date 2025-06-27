package com.fitness.activityservice.service;

import com.fitness.activityservice.dto.ActivityRequest;
import com.fitness.activityservice.dto.ActivityResponse;
import com.fitness.activityservice.model.Activity;
import com.fitness.activityservice.repository.ActivityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityService {
    private final UserValidationService userValidationService;
    private final ActivityRepository activityRepository;
private final RabbitTemplate rabbitTemplate;
  @Value("${rabbitmq.exchange.name}")
    private String exchange;
    @Value("${rabbitmq.routing.key}")
    private String routingKey;

    public ActivityResponse tractActivity(ActivityRequest request) {
        boolean isValidUser = userValidationService.validateUser(request.getUserId());

        if(!isValidUser){
            throw new RuntimeException("Invalid User: "+ request.getUserId());

        }
        Activity activity = Activity.builder().userId(request.getUserId()).type(request.getType()).duration(request.getDuration()).caloriesBurned(request.getCaloriesBurned()).startTime(request.getStartTime()).additionalMetrics(request.getAdditionalMetrics()).build();

        Activity savedActivity = activityRepository.save(activity);
try{

    // Publish activity to RabbitMQ
        rabbitTemplate.convertAndSend(exchange,routingKey,savedActivity);
}catch (Exception e){
log.error("Failde to publish activity to rabit MQ",e);
}
        return mapToResponse(savedActivity);
    }

    private ActivityResponse mapToResponse(Activity activity){

        ActivityResponse response = new ActivityResponse();
        response.setId(activity.getId());
        response.setUserId(activity.getUserId());
        response.setType(activity.getType());
         response.setDuration(activity.getDuration());
         response.setCaloriesBurned(activity.getCaloriesBurned());
         response.setStartTime(activity.getStartTime());
         response.setAdditionalMetrics(activity.getAdditionalMetrics());
         response.setCreatedAt(activity.getCreatedAt());
         response.setUpdatedAt(activity.getUpdatedAt());


        return response;
    }

    public List<ActivityResponse> getUserActivity(String userId) {

        List<Activity> activities = activityRepository.findByUserId(userId);
        return activities.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public ActivityResponse getActivityById(String activityId) {
        return activityRepository.findById(activityId).map(this::mapToResponse)
                .orElseThrow(()-> new RuntimeException("Activity not found with id: "+ activityId));
    }
}
