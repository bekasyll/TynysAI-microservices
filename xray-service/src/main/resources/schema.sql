CREATE TABLE IF NOT EXISTS xray_analyses (
    id BIGSERIAL PRIMARY KEY,
    patient_id UUID,
    original_file_name VARCHAR(255) NOT NULL,
    stored_file_path VARCHAR(500) NOT NULL,
    content_type VARCHAR(50) NOT NULL,
    file_size_bytes BIGINT,
    status VARCHAR(20) NOT NULL,
    ai_primary_diagnosis VARCHAR(30),
    ai_confidence DOUBLE PRECISION,
    ai_all_predictions_json TEXT,
    ai_findings TEXT,
    ai_detected_abnormalities TEXT,
    assigned_doctor_id UUID,
    validated_by_doctor_id UUID,
    doctor_diagnosis VARCHAR(30),
    doctor_notes TEXT,
    validated_at TIMESTAMP,
    patient_notes VARCHAR(500),
    uploaded_at TIMESTAMP,
    analyzed_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_xray_patient ON xray_analyses (patient_id);
CREATE INDEX IF NOT EXISTS idx_xray_assigned_doctor ON xray_analyses (assigned_doctor_id);