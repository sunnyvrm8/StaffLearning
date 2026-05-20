---
title: Case Study 08 — Video Streaming
description: Design a video streaming service with CDN delivery, adaptive bitrate, and upload ingestion.
---

# Video Streaming

A video streaming system must ingest video uploads, store and serve chunks through a CDN, and support adaptive bitrate playback. The system should optimize for delivery cost, startup latency, and content availability.

## Problem framing

- **Users:** viewers, content creators, playback clients
- **Peak load:** millions of concurrent viewers, tens of TB per hour served
- **Critical path:** start playback in <2s and keep buffer underrun low
- **Business goals:** minimize egress cost, maximize quality, support regional delivery

## Requirements

- Accept user uploads and transcode assets into multiple bitrates
- Serve video chunks efficiently through an edge CDN
- Support adaptive bitrate and playback metadata
- Enforce access control and regional restrictions
- Track viewing metrics and cache hit ratios

## Key constraints

- Video storage is expensive and must be tiered
- Transcoding is CPU intensive and latency sensitive for uploads
- Edge caches must be populated before playback starts
- Bandwidth spikes from popular content can overwhelm origins
- Live streaming introduces even tighter latency and segmenting demands

## Architecture overview

- **Upload service** receives source video and schedules transcoding.
- **Transcoding pipeline** generates bitrate variants and thumbnails.
- **Origin storage** stores segments and serves the CDN.
- **CDN edge** delivers chunks globally with cache-control.
- **Playback service** orchestrates manifest generation and DRM checks.

## API sketch

| Method | Path | Notes |
|--------|------|-------|
| POST | /upload | Start video ingest |
| GET | /manifest/{id} | Retrieve playback manifest |
| GET | /segment/{id}/{quality} | Serve video chunk |
| GET | /stats/{id} | Playback analytics |

## Data model

- `VideoAsset`
  - `assetId`
  - `ownerId`
  - `variants`
  - `status`
  - `createdAt`

- `StreamSegment`
  - `segmentId`
  - `assetId`
  - `quality`
  - `duration`
  - `location`

- `PlaybackSession`
  - `sessionId`
  - `assetId`
  - `userId`
  - `bitrate`
  - `startedAt`

## Diagrams

- [Context diagram](./diagrams/context.md)
- [Components diagram](./diagrams/components.md)
- [Core flow diagram](./diagrams/core-flow.md)

## Reliability and failure modes

- **Origin overload:** use CDN edge and origin shielding
- **Transcode failures:** retry on failure and fail fast to the uploader with logs
- **Cache misses on hot content:** pre-warm or prefetch popular assets to the edge
- **Regional availability:** replicate assets selectively and enforce geo restrictions at edge
- **Playback jitter:** provide multi-bitrate manifests and client-side buffer control

## If I had two more weeks

- Add a real-time upload progress and release pipeline
- Add analytics-driven prefetching of trending content
- Add live streaming and low-latency chunking support

## Three scale triggers

1. **Viral video spikes** → add automatic edge pre-warming and origin auto-scaling
2. **Storage growth** → tier older assets to cold storage and optimize retrieval paths
3. **Playback quality demand** → support more bitrate variants and optimize manifest size

## Interview prompts

- Why use a CDN for video delivery and what does origin shielding do?
- How would you design the upload and transcoding pipeline to avoid blocking user requests?
- What are the cost vs latency trade-offs in storing video at multiple qualities?
