package bff.service;

import bff.dto.EndRideRequest;
import bff.dto.JsonResponse;
import bff.dto.StartRideRequest;

public interface RideService {

    JsonResponse start(String jwt, StartRideRequest request);

    JsonResponse finish(String jwt, long rideId, EndRideRequest request);

    JsonResponse pay(String jwt, long rideId);

    JsonResponse findByUser(String jwt, long userId);
}
