# SecureLaw AI Gateway️

> **Secure document text extraction for legal professionals — built with Privacy by Design.**

The Secure Law AI Gateway is a secure, full-stack document processing utility that allows legal professionals to upload sensitive documents, extract their text content using Apache Tika, and preview the results in real-time. No sensitive data is ever persisted on the server disk.

---

##  Key Features

- **Multi-Format Text Extraction** — Supports `.pdf`, `.docx`, and `.txt` files via Apache Tika.
- **Zero-Persistence Security** — Documents are processed as in-memory streams. Files are never written to disk, meeting strict legal compliance standards.
- **Interactive Preview UI** — Immediate character count and text preview before the anonymization step.
- **High-Capacity Processing** — Handles uploads up to **20MB** with real-time progress tracking.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Frontend | Angular 17+, RxJS, Tailwind CSS |
| Backend | Spring Boot 3.x (Java 21) |
| Extraction Engine | Apache Tika |
| API Protocol | REST (Multipart FormData) |

---

##  User Story

> *As a Lawyer, I want to upload `.pdf`, `.docx`, `.txt` files and see extracted text preview, so that I can submit document content for anonymization.*

### Acceptance Criteria

- `POST /api/documents/upload` (multipart) returns `{ uploadId, previewText, charCount }`.
- Accepts `.pdf`, `.docx`, `.txt`; rejects all other formats with `415`.
- Extracted file is removed from disk immediately after extraction (zero-persistence).

---

## 📂 API Reference

### Upload Document

Processes a legal document and returns extracted text metadata.

```
POST /api/documents/upload
Content-Type: multipart/form-data
```

**Validation:**
- Rejects files larger than **20MB** → `413 Payload Too Large`
- Rejects unsupported file formats → `415 Unsupported Media Type`

**Example Response `200 OK`:**

```json
{
  "uploadId": "uuid-789-abc",
  "previewText": "This is the extracted content of the legal brief...",
  "charCount": 14502
}
```

**Status Codes:**

| Code | Meaning |
|---|---|
| `200 OK` | Extraction successful |
| `413 Payload Too Large` | File exceeds 20MB limit |
| `415 Unsupported Media Type` | File type not supported |

---

##  Technical Tasks

- [x] Integrate **Apache Tika** for text extraction
- [x] Validate file size (max 20MB) and MIME type
- [x] Implement zero-persistence stream processing (no disk writes)
- [x] Build front-end upload component with progress indicator and text preview

---

## ✅ Definition of Done

- Can upload a 20MB PDF and display a text preview successfully.
- No file is persisted on disk at any point during or after extraction.

---

##  Getting Started

### Prerequisites

- Java 21+
- Node.js 18+ / Angular CLI
- Apache Tika (bundled via Spring Boot dependency)

### Backend

```bash
cd backend
./mvnw spring-boot:run
```

### Frontend

```bash
cd frontend
npm install
ng serve
```

The app will be available at `http://localhost:4200`.

---

##  Security & Compliance

DocuShield is built around a **Privacy by Design** philosophy:

- All document content is processed exclusively in memory.
- No temporary files are created on the server filesystem.
- File type and size are validated before any processing begins.
- Designed to support downstream anonymization pipelines for legal workflows.

---

## 📄 License

This project is licensed under the [MIT License](LICENSE).