package com.telemetryparser.datatransformation.util;

import com.telemetryparser.util.math.Vec3;
import java.util.Date;

public class CoordUtil
{
	public static Vec3 getECEF(double latitude, double longitude, double altitude)
	{
		double latRad = Math.toRadians(latitude);
		double lonRad = Math.toRadians(longitude);

		double a = 6378137.0;
		double f = 1.0 / 298.257223563;
		double e2 = 2 * f - f * f;

		double sinLat = Math.sin(latRad);
		double cosLat = Math.cos(latRad);
		double N = a / Math.sqrt(1.0 - e2 * sinLat * sinLat);

		double xECEF = ((N + altitude) * cosLat * Math.cos(lonRad)) / 1000;
		double yECEF = ((N + altitude) * cosLat * Math.sin(lonRad)) / 1000;
		double zECEF = ((N * (1.0 - e2) + altitude) * sinLat) / 1000;

		return new Vec3(xECEF, yECEF, zECEF);
	}

	public static Vec3 getECEFVelocities(Vec3 currentECEFPosition, Vec3 originECEF, double originAzimuth, double altitudeChange, double overallSpeed)
	{
		Vec3 upOrigin = originECEF.copy().normalize();
		Vec3 zAxis = new Vec3(0, 0, 1);

		Vec3 eastOrigin = zAxis.cross(upOrigin).normalize();
		Vec3 northOrigin = upOrigin.cross(eastOrigin).normalize();

		double azRad = Math.toRadians(originAzimuth);

		Vec3 headingOrigin = northOrigin.scale(Math.cos(azRad)).add(eastOrigin.scale(Math.sin(azRad)));
		Vec3 gcNormal = originECEF.cross(headingOrigin).normalize();
		Vec3 upCurrent = currentECEFPosition.copy().normalize();
		Vec3 tangent = gcNormal.cross(currentECEFPosition).normalize();

		double horizontalSpeed;

		if (Math.abs(altitudeChange) < overallSpeed)
		{
			horizontalSpeed = Math.sqrt(overallSpeed * overallSpeed - altitudeChange * altitudeChange);
		}
		else
		{
			horizontalSpeed = 0.0;
		}

		Vec3 horizontalVelocityECEF = tangent.scale(horizontalSpeed);
		Vec3 verticalVelocityECEF = upCurrent.scale(altitudeChange);

		return horizontalVelocityECEF.add(verticalVelocityECEF);
	}

	public static Vec3 getTranslatedECEF(Vec3 position, Vec3 velocity, double seconds)
	{
		if (seconds <= 0)
		{
			throw new IllegalArgumentException("Time in seconds must be positive. Value given: " + seconds);
		}

		Vec3 displacement = velocity.copy().scale(1.75*seconds/3600d);

		return position.copy().add(displacement);
	}

	public static Vec3 getLatLonAlt(Vec3 positionECEF)
	{
		final double a = 6378137.0; //Semi Major Axis
		final double f = 1.0 / 298.257223563; //Flattening
		final double e2 = 2 * f - f * f; //Eccentricity^2

		double X = positionECEF.x * 1000;
		double Y = positionECEF.y * 1000;
		double Z = positionECEF.z * 1000;

		double lon = Math.atan2(Y, X);
		double r = Math.sqrt(X * X + Y * Y);
		double lat = Math.atan2(Z, (1.0 - e2) * r);

		double latPrev;
		double sinLat;
		double N;

		do //geocentric -> geodetic (Bowring's formula)
		{
			latPrev = lat;
			sinLat = Math.sin(lat);
			N = a / Math.sqrt(1.0 - e2 * sinLat * sinLat);
			lat = Math.atan2(Z + e2 * N * sinLat, r);
		}
		while (Math.abs(lat - latPrev) > 1e-12);

		sinLat = Math.sin(lat);
		N = a / Math.sqrt(1.0 - e2 * sinLat * sinLat);

		double alt = r / Math.cos(lat) - N; //relative to WGS84 ellipsoid
		double latDeg = Math.toDegrees(lat);
		double lonDeg = Math.toDegrees(lon);

		return new Vec3(latDeg, lonDeg, alt);
	}

	public static Vec3 getTranslatedECEF(Vec3 position, Vec3 velocity)
	{
		return getTranslatedECEF(position, velocity, 1000);
	}

	public static double getGroundSpeed(Vec3 position, Vec3 velocity)
	{
		double positionLength = position.length();
		if (positionLength < 1e-9)
		{
			return velocity.length();
		}

		Vec3 radialUnit = position.copy().normalize();
		double radialSpeed = velocity.dot(radialUnit);
		Vec3 horizontalVelocity = velocity.copy().sub(radialUnit.scale(radialSpeed));

		return horizontalVelocity.length();
	}

	public static Vec3 getECEFVelocities(Vec3 ECEF1, Vec3 ECEF2, int ms)
	{
		if (ms <= 0)
		{
			throw new IllegalArgumentException("Time in milliseconds must be positive.");
		}
		Vec3 deltaPosition = ECEF2.copy().sub(ECEF1);

		float timeInSeconds = ms / 1000f;

		return deltaPosition.scale(1.0f / timeInSeconds);
	}

	public static Vec3 getECEFVelocities(Vec3 ECEF1, Vec3 ECEF2)
	{
		return getECEFVelocities(ECEF1, ECEF2, 1000);
	}

	public static Vec3 getECI(double latitude, double longitude, double altitude, Date time)
	{
		return getECICoordinate(latitude, longitude, altitude, computeSiderealAngleDegrees(time.getTime(), getDUT1(time)));
	}

	private static Vec3 getECICoordinate(double initialLatitude, double initialLongitude, double initialAltitude, double siderealAngleRadians)
	{

		Vec3 ECEF = getECEF(initialLatitude, initialLongitude, initialAltitude);
		double cosTheta = Math.cos(siderealAngleRadians);
		double sinTheta = Math.sin(siderealAngleRadians);

		double xECI = ECEF.x * cosTheta - ECEF.y * sinTheta;
		double yECI = ECEF.x * sinTheta + ECEF.y * cosTheta;
		double zECI = ECEF.z;

		return new Vec3(xECI, yECI, zECI);
	}

	public static double getDUT1(Date date)
	{
		// from: https://datacenter.iers.org/data/latestVersion/bulletinA.txt
		// MJD = Julian Date - 2 400 000.5 days
		// UT2-UT1 = 0.022*sin(2*pi*T) - 0.012*cos(2*pi*T) - 0.006*sin(4*pi*T) + 0.007*cos(4*pi*T)
		// T = date in Besselian years.
		// Besselian year T = 1900.0 + (Julian date − 2415020.31352) / 365.242198781

		double jd = (date.getTime() / 86400000.0) + 2440587.5;
		double mjd = jd - 2400000.5;
		double T = 1900.0 + (jd - 2415020.31352) / 365.2421988;
		double ut2MinusUt1 = 0.022 * Math.sin(2 * Math.PI * T) - 0.012 * Math.cos(2 * Math.PI * T) - 0.006 * Math.sin(4 * Math.PI * T) + 0.007 * Math.cos(4 * Math.PI * T);

		//IERS: UT1-UTC approximate =  0.0351 + 0.00013 (MJD - 60699) - (UT2-UT1)

		return 0.0351 + 0.00013 * (mjd - 60699.0) - ut2MinusUt1;
	}

	private static double getJD(long ms)
	{
		return (ms / 86400000.0) + 2440587.5;
	}

	private static double getMJD(long ms)
	{
		return getJD(ms) - 2400000.5;
	}

	private static double computeSiderealAngleDegrees(long utcMillis, double DUT1)
	{

		double jd = getJD(utcMillis);
		double jdUT1 = jd + (DUT1 / 86400.0);
		double dUT1 = jdUT1 - 2451545.0;
		double ERA = 2.0 * Math.PI * (0.7790572732640 + 1.00273781191135448 * dUT1);
		ERA = normalizeRadians(ERA);
		double EE = computeEquationOfEquinoxes(jdUT1);
		double GAST = ERA + EE;
		GAST = normalizeRadians(GAST);

		return Math.toDegrees(GAST);
	}

	private static double normalizeRadians(double angleRadians)
	{
		angleRadians = angleRadians % (2.0 * Math.PI);
		if (angleRadians < 0)
		{
			angleRadians += (2.0 * Math.PI);
		}
		return angleRadians;
	}

	public static double computeEquationOfEquinoxes(double jdUT1)
	{
		double deltaT = getDeltaT(jdUT1);
		double jdTT = jdUT1 + (deltaT / 86400.0);
		double T = (jdTT - 2451545.0) / 36525.0;

		double M = 357.5291092 + 35999.0502909 * T - 0.0001536 * T * T + 1.0 / 24490000.0 * T * T * T;
		double Mprime = 134.9633964 + 477198.8675055 * T + 0.0087414 * T * T + 1.0 / 69699.0 * T * T * T - 1.0 / 14712000.0 * T * T * T * T;
		double D = 297.8501921 + 445267.1114034 * T - 0.0018819 * T * T + 1.0 / 545868.0 * T * T * T - 1.0 / 113065000.0 * T * T * T * T;
		double F = 93.2720950 + 483202.0175233 * T - 0.0036539 * T * T - 1.0 / 3526000.0 * T * T * T + 1.0 / 863310000.0 * T * T * T * T;
		double Omega = 125.0445479 - 1934.1362891 * T + 0.0020754 * T * T + 1.0 / 467441.0 * T * T * T - 1.0 / 60616000.0 * T * T * T * T;

		double Mrad = Math.toRadians(M);
		double MprimeRad = Math.toRadians(Mprime);
		double Drad = Math.toRadians(D);
		double Frad = Math.toRadians(F);
		double OmegaRad = Math.toRadians(Omega);

		int[][] nutationCoeff =
			{
				{0, 0, 0, 0, 1, -725, +85},
				{0, 0, 2, -2, 2, +217, -95},
				{0, 0, 2, 0, 2, +31, -1},
				{-2, 0, 2, 0, 1, +27, -1},
				{0, 1, 0, 0, 0, -17, +0}
			};

		double dPsiSum = 0.0;

		for (int[] c : nutationCoeff)
		{
			int a = c[0], b = c[1], cc = c[2], d = c[3], e = c[4];
			double sinCoeff = c[5] * 0.0001;
			double arg = a * Drad + b * Mrad + cc * MprimeRad + d * Frad + e * OmegaRad;
			dPsiSum += sinCoeff * Math.sin(arg);
		}

		double dPsi = dPsiSum;

		double meanObliquityArcSec = 84381.448 - 4680.93 * T - 1.55 * T * T + 1999.25
			* T * T * T / 10000.0 - 51.38 * T * T * T * T / 10000.0 - 249.67 * T * T * T * T * T / 10000.0 - 39.05 * T * T * T * T * T * T / 10000.0;
		double eps0 = Math.toRadians(meanObliquityArcSec / 3600.0);

		double eoe = dPsi * Math.cos(eps0) + 0.00264 * Math.sin(OmegaRad) + 0.000063 * Math.sin(2.0 * OmegaRad);

		return eoe;
	}


	private static double getDeltaT(double jd)
	{
		double year = 2000.0 + (jd - 2451545.0) / 365.25;
		double T = year - 2000.0;

		return 63.0 + 0.3345 * T + 0.060374 * T * T;
	}


}
