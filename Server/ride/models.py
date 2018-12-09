from django.db import models

class User(models.Model):
    id_ = models.AutoField(primary_key=True)
    name = models.CharField(max_length=200)
    facebookProfileId = models.CharField(max_length=200)

class Ride(models.Model):
    id_ = models.AutoField(primary_key=True)
    driverId: models.IntegerField()
    eventId: models.IntegerField()
    origin: models.CharField(max_length=200)
    origin: models.CharField(max_length=200)
    departureTime: models.DateTimeField(default=timezone.now)
    carModel: models.CharField(max_length=200)
    carColor: models.CharField(max_length=200)
    passengerCount: models.IntegerField()
    extraDetails: models.textField()

class Event(models.Model):
    id_ = models.AutoField(primary_key=True)
    name = models.CharField(max_length=200)
    location: models.CharField(max_length=200)
    datetime: models.DateTimeField(default=timezone.now)
    facebookEventId: models.CharField(max_length=200)

class Pickup(models.Model):
    id_: models.AutoField(primary_key=True)
    userId: models.IntegerField()
    rideId: models.IntegerField()
    pickupSpot: models.CharField(max_length=200)
    pickupTime: models.DateTimeField(default=timezone.now)

class TimeOfDay(models.Model):
    hours: models.IntegerField()
    minutes: models.IntegerField()

