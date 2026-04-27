CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    middle_name VARCHAR(255),
    phone_number VARCHAR(20) NOT NULL,
    role VARCHAR(20) NOT NULL,
    enabled BOOLEAN NOT NULL,
    email_verified BOOLEAN NOT NULL,
    avatar_path VARCHAR(500),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS patient_profiles (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE,
    date_of_birth DATE,
    gender VARCHAR(10),
    blood_type VARCHAR(15),
    height_cm DOUBLE PRECISION,
    weight_kg DOUBLE PRECISION,
    allergies VARCHAR(1000),
    chronic_diseases VARCHAR(1000),
    emergency_contact_name VARCHAR(100),
    emergency_contact_phone VARCHAR(20),
    address VARCHAR(255),
    insurance_number VARCHAR(50),
    occupation VARCHAR(100),
    smoker BOOLEAN,
    alcohol_user BOOLEAN,
    medical_history VARCHAR(2000),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS doctor_profiles (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE,
    date_of_birth DATE,
    gender VARCHAR(10),
    specialization VARCHAR(255) NOT NULL,
    license_number VARCHAR(255) UNIQUE,
    hospital_name VARCHAR(255),
    department VARCHAR(255),
    years_of_experience INTEGER,
    bio VARCHAR(500),
    education VARCHAR(500),
    approved BOOLEAN NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);