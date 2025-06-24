package com.fitness.activityservice.controller;


import com.fitness.activityservice.dto.ActivityRequest;
import com.fitness.activityservice.dto.ActivityResponse;
import com.fitness.activityservice.model.Activity;
import com.fitness.activityservice.repository.ActivityRepository;
import com.fitness.activityservice.service.ActivityService;
import lombok.AllArgsConstructor;
import org.apache.coyote.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/activities")
@AllArgsConstructor
public class ActivityController {
    private final ActivityRepository activityRepository;
    private ActivityService activityService;

    @PostMapping
    public ResponseEntity<ActivityResponse> trackActivity(@RequestBody ActivityRequest request) {

        return ResponseEntity.ok(activityService.tractActivity(request));

    }

    @GetMapping
    public ResponseEntity<List<ActivityResponse>> getUserActivity(@RequestHeader("X-User-ID") String userId) {

        return ResponseEntity.ok(activityService.getUserActivity(userId));


    }


    @GetMapping("{activityId}")
    public ResponseEntity<ActivityResponse> getActivity(@PathVariable String activityId) {

        return ResponseEntity.ok(activityService.getActivityById(activityId));


    }
}