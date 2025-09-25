# Voice Food Log Endpoint Test

## Issues Fixed:

### 1. **loggedAt Format Issue** ✅
- **Problem**: AI returned `YYYY-MM-DDTHH:mm:ss` but `FoodLogCreateRequest` expected `yyyy-MM-dd'T'HH:mm:ss'Z'`
- **Fix**: Updated AI prompt to return `YYYY-MM-DDTHH:mm:ssZ` format and added parsing logic to handle both formats

### 2. **Nutrition Calculation Issue** ✅
- **Problem**: AI nutrition data is per 100g, but we were setting `quantityPerUnit = 1.0`, causing massive over-calculation
- **Example**: 200g chicken would calculate as `200/1.0 * 165 = 33,000 calories` instead of `200/100.0 * 165 = 330 calories`
- **Fix**: Changed `quantityPerUnit` to `100.0` since AI nutrition data is per 100g

## Test Scenario:

**Input**: "I ate 200 grams of grilled chicken breast for lunch"

**Expected Flow**:
1. AI parses: `{"foodName": "grilled chicken breast", "quantity": 200, "unit": "grams", "mealType": "lunch", "loggedAt": "2024-01-15T12:00:00Z", "nutrition": {"caloriesPer100g": 165, "proteinPer100g": 31, ...}}`
2. Creates FoodItem with `quantityPerUnit = 100.0`, `caloriesPerUnit = 165`
3. Creates FoodLog with `quantity = 200`
4. Calculates: `scale = 200/100.0 = 2.0`, `calories = 2.0 * 165 = 330 calories` ✅

**Before Fix**: Would have calculated `200/1.0 * 165 = 33,000 calories` ❌
**After Fix**: Correctly calculates `200/100.0 * 165 = 330 calories` ✅

## Summary:
Both the `loggedAt` timezone format and nutrition calculation issues have been resolved. The voice food log endpoint should now work correctly with proper timestamp parsing and accurate nutrition calculations.
