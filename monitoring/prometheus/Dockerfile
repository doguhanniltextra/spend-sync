FROM prom/prometheus:v2.51.0

# Konfigürasyonu container içine kopyala
COPY prometheus.yml /etc/prometheus/prometheus.yml

# En düşük yetki: Prometheus resmi nobody kullanıcısı (UID 65534)
USER nobody

EXPOSE 9090

ENTRYPOINT [ "/bin/prometheus" ]
CMD [ "--config.file=/etc/prometheus/prometheus.yml", \
      "--storage.tsdb.path=/prometheus", \
      "--web.enable-lifecycle", \
      "--storage.tsdb.retention.time=15d" ]
