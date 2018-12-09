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

