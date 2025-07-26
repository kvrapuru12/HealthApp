#!/bin/bash

echo "🧹 HealthApp Database Cleanup and Restart Script"
echo "================================================"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Database configuration
DB_HOST="localhost"
DB_PORT="3306"
DB_USER="root"
DB_PASSWORD="PresentationSkills232@"
DB_NAME="healthapp"

echo -e "${YELLOW}Step 1: Dropping all tables from local database...${NC}"

# Create temporary SQL file for dropping tables
cat > temp_drop_tables.sql << 'EOF'
USE healthapp;

-- Disable foreign key checks temporarily
SET FOREIGN_KEY_CHECKS = 0;

-- Drop all tables
DROP TABLE IF EXISTS activity_entries;
DROP TABLE IF EXISTS food_entries;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS flyway_schema_history;

-- Re-enable foreign key checks
SET FOREIGN_KEY_CHECKS = 1;

-- Verify tables are dropped
SHOW TABLES;
EOF

# Execute the drop tables script
if mysql -h $DB_HOST -P $DB_PORT -u $DB_USER --password="$DB_PASSWORD" < temp_drop_tables.sql; then
    echo -e "${GREEN}✅ All tables dropped successfully!${NC}"
else
    echo -e "${RED}❌ Failed to drop tables${NC}"
    exit 1
fi

# Clean up temporary file
rm -f temp_drop_tables.sql

echo -e "${YELLOW}Step 2: Verifying database is clean...${NC}"
if mysql -h $DB_HOST -P $DB_PORT -u $DB_USER --password="$DB_PASSWORD" -e "USE $DB_NAME; SHOW TABLES;" | grep -q .; then
    echo -e "${RED}❌ Tables still exist in database${NC}"
    exit 1
else
    echo -e "${GREEN}✅ Database is clean (no tables found)${NC}"
fi

echo -e "${YELLOW}Step 3: Starting HealthApp application...${NC}"
echo -e "${YELLOW}This will recreate all tables with the correct schema using Flyway migrations${NC}"
echo -e "${YELLOW}Press Ctrl+C to stop the application when done${NC}"
echo ""

# Start the application
echo -e "${GREEN}🚀 Starting HealthApp...${NC}"
echo -e "${YELLOW}The application will now create all tables with the correct schema${NC}"
echo -e "${YELLOW}Press Ctrl+C to stop the application when done${NC}"
echo ""
mvn spring-boot:run 