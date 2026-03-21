# Set the directory for this project so make deploy need not receive PROJECT_DIR
PROJECT_DIR         := ether-ai-openai
PROJECT_GROUP_ID    := dev.rafex.ether.ai
PROJECT_ARTIFACT_ID := ether-ai-openai
DEPENDENCY_COORDS   := ether-ai-core.version=dev.rafex.ether.ai:ether-ai-core ether-json.version=dev.rafex.ether.json:ether-json

# Include shared build logic
include ../build-helpers/common.mk
include ../build-helpers/git.mk
