FROM grafana/grafana:10.4.0

USER root

# Provisioning yapılandırmalarını imaj içine kopyala
COPY provisioning/ /etc/grafana/provisioning/

# İzinleri Grafana kullanıcısına devret (UID 472, GID 472)
RUN chown -R 472:472 /etc/grafana/provisioning

# En düşük yetkili Grafana kullanıcısına dön
USER 472

EXPOSE 3000
