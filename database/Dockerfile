FROM postgres:15-alpine

# Install additional tools for migration
RUN apk add --no-cache bash curl

# Copy migration system files
COPY migrate.sql /docker-entrypoint-initdb.d/00-migrate.sql
COPY run_migrations.sh /usr/local/bin/run_migrations.sh
COPY migrations/ /docker-entrypoint-initdb.d/migrations/

# Copy original init script for fallback
COPY init.sql /docker-entrypoint-initdb.d/99-init-fallback.sql

# Make scripts executable
RUN chmod +x /usr/local/bin/run_migrations.sh

# Create custom entrypoint that runs migrations
COPY docker-entrypoint.sh /usr/local/bin/custom-entrypoint.sh
RUN chmod +x /usr/local/bin/custom-entrypoint.sh

# Set custom entrypoint
ENTRYPOINT ["/usr/local/bin/custom-entrypoint.sh"]
CMD ["postgres"]