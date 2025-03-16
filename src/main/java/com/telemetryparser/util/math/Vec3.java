package com.telemetryparser.util.math;


public class Vec3
{

	public double x;
	public double y;
	public double z;

	public Vec3()
	{
		this(0.0f, 0.0f, 0.0f);
	}

	public Vec3(double x, double y, double z)
	{
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public Vec3(Vec3 other)
	{
		this(other.x, other.y, other.z);
	}

	public Vec3 set(double x, double y, double z)
	{
		this.x = x;
		this.y = y;
		this.z = z;
		return this;
	}

	public Vec3 set(Vec3 other)
	{
		return set(other.x, other.y, other.z);
	}

	public Vec3 add(Vec3 other)
	{
		this.x += other.x;
		this.y += other.y;
		this.z += other.z;
		return this;
	}

	public Vec3 add(double x, double y, double z)
	{
		this.x += x;
		this.y += y;
		this.z += z;
		return this;
	}

	public Vec3 sub(Vec3 other)
	{
		this.x -= other.x;
		this.y -= other.y;
		this.z -= other.z;
		return this;
	}

	public Vec3 sub(double x, double y, double z)
	{
		this.x -= x;
		this.y -= y;
		this.z -= z;
		return this;
	}

	public Vec3 scale(double scalar)
	{
		this.x *= scalar;
		this.y *= scalar;
		this.z *= scalar;
		return this;
	}

	public double dot(Vec3 other)
	{
		return this.x * other.x + this.y * other.y + this.z * other.z;
	}

	public Vec3 cross(Vec3 other)
	{
		double nx = this.y * other.z - this.z * other.y;
		double ny = this.z * other.x - this.x * other.z;
		double nz = this.x * other.y - this.y * other.x;
		return new Vec3(nx, ny, nz);
	}

	public double lengthSquared()
	{
		return x * x + y * y + z * z;
	}

	public double length()
	{
		return Math.sqrt(lengthSquared());
	}

	public Vec3 normalize()
	{
		double len = length();
		if (len != 0.0f)
		{
			this.x /= len;
			this.y /= len;
			this.z /= len;
		}
		return this;
	}

	public Vec3 normalized()
	{
		double len = length();
		if (len == 0.0f)
		{
			return new Vec3(this);
		}
		return new Vec3(this.x / len, this.y / len, this.z / len);
	}

	public double distanceTo(Vec3 other)
	{
		double dx = other.x - this.x;
		double dy = other.y - this.y;
		double dz = other.z - this.z;
		return Math.sqrt(dx * dx + dy * dy + dz * dz);
	}

	public Vec3 lerp(Vec3 other, double alpha)
	{
		return new Vec3(
			this.x + alpha * (other.x - this.x),
			this.y + alpha * (other.y - this.y),
			this.z + alpha * (other.z - this.z)
		);
	}

	public Vec3 copy()
	{
		return new Vec3(this);
	}

	@Override
	public int hashCode()
	{
		long result = 17;
		result = 31 * result + Double.doubleToLongBits(x);
		result = 31 * result + Double.doubleToLongBits(y);
		result = 31 * result + Double.doubleToLongBits(z);
		return (int) result;
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o)
		{
			return true;
		}
		if (!(o instanceof Vec3 other))
		{
			return false;
		}
		return Double.compare(other.x, x) == 0 && Double.compare(other.y, y) == 0 && Double.compare(other.z, z) == 0;
	}

	@Override
	public String toString()
	{
		return "Vec3(" + x + ", " + y + ", " + z + ")";
	}
}
