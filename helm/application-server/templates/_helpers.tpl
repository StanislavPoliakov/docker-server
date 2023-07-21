{{/* Generate basic labels */}}
{{- define "application-service.labels" }}
  labels:
    meta.helm.sh/release-name: {{ .Release.Name }}
    meta.helm.sh/release-namespace: {{ .Values.settings.main.namespace }}
{{- end }}

{{/* Generate basic annotations */}}
{{- define "application-service.annotations" }}
  annotations:
    app.kubernetes.io/managed-by: Helm
{{- end }}