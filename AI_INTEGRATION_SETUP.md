# 🤖 AI Integration Setup Guide

## Quick Start (5 Minutes)

### 1. Get Your Hugging Face API Key

1. **Sign up** at [huggingface.co](https://huggingface.co)
2. **Go to tokens**: [huggingface.co/settings/tokens](https://huggingface.co/settings/tokens)
3. **Create new token**:
   - Name: `CourseFinder-AI-Validation`
   - Type: `Read` (free tier)
4. **Copy the token** (starts with `hf_...`)

### 2. Set Environment Variable

```bash
# Windows (Command Prompt)
set HUGGINGFACE_API_KEY=hf_your_token_here

# Windows (PowerShell)
$env:HUGGINGFACE_API_KEY="hf_your_token_here"

# Linux/Mac
export HUGGINGFACE_API_KEY=hf_your_token_here
```

### 3. Test Your Setup

Start your Spring Boot application and test:

```bash
# Test current configuration
GET http://localhost:8080/api/ai-validation/test/configuration

# Compare different models
POST http://localhost:8080/api/ai-validation/test/compare-models
```

## Model Selection Guide

### 🚀 **For Development/Testing** (Recommended to start)
```properties
huggingface.model.validation=microsoft/DialoGPT-medium
huggingface.model.education=google/t5-base
```
- **Cost**: Free (1,000 requests/month)
- **Speed**: Fast
- **Quality**: Good for testing

### 🎯 **For Production** (High Quality)
```properties
huggingface.model.validation=microsoft/DialoGPT-large
huggingface.model.education=EleutherAI/gpt-neo-2.7B
```
- **Cost**: Paid plan required
- **Speed**: Medium
- **Quality**: Excellent

### ⚡ **For High Volume** (Fast & Cheap)
```properties
huggingface.model.validation=facebook/blenderbot-400M-distill
huggingface.model.education=microsoft/DialoGPT-medium
```
- **Cost**: Low
- **Speed**: Very Fast
- **Quality**: Good enough

## Testing Your Models

### 1. Test Current Configuration
```bash
curl -X GET http://localhost:8080/api/ai-validation/test/configuration
```

Expected response:
```json
{
  "status": "SUCCESS",
  "configurationTest": {
    "validationModel": "microsoft/DialoGPT-medium",
    "educationModel": "google/t5-base",
    "overallWorking": true
  }
}
```

### 2. Compare Different Models
```bash
curl -X POST http://localhost:8080/api/ai-validation/test/compare-models
```

This will test 4 different models and give you recommendations.

### 3. Validate Real Content
```bash
# Test course validation
curl -X POST http://localhost:8080/api/ai-validation/validate/courses \
  -H "Content-Type: application/json" \
  -d '{"coursePath": "BS Computer Science: A program in computing systems"}'

# Test career validation
curl -X POST http://localhost:8080/api/ai-validation/validate/careers \
  -H "Content-Type: application/json" \
  -d '{"careerSuggestions": "Software Engineer: Develops software applications"}'
```

## Troubleshooting

### ❌ "API Key not found"
- Make sure you set the environment variable correctly
- Restart your Spring Boot application after setting the variable

### ❌ "Model not available"
- Check if the model name is correct
- Some models might be temporarily unavailable
- Try a different model from the recommendations

### ❌ "Timeout error"
- Increase timeout in `application.properties`:
```properties
huggingface.timeout=60000
```

### ❌ "Rate limit exceeded"
- You've hit the free tier limit (1,000 requests/month)
- Upgrade to paid plan or wait until next month

## Cost Optimization Tips

1. **Start with free tier** for development
2. **Cache validation results** to avoid repeated calls
3. **Use smaller models** for simple validations
4. **Batch multiple validations** together
5. **Monitor usage** to avoid unexpected costs

## Production Deployment

### Environment Variables for Production
```bash
HUGGINGFACE_API_KEY=hf_your_production_token
HUGGINGFACE_VALIDATION_MODEL=microsoft/DialoGPT-large
HUGGINGFACE_EDUCATION_MODEL=EleutherAI/gpt-neo-2.7B
HUGGINGFACE_TIMEOUT=60000
```

### Monitoring
- Set up logging for API calls
- Monitor response times
- Track validation success rates
- Set up alerts for API failures

## Next Steps

1. ✅ **Test your setup** with the configuration endpoint
2. ✅ **Compare models** to find the best fit
3. ✅ **Integrate validation** into your email/PDF services
4. ✅ **Monitor performance** and adjust models as needed

## Support

If you encounter issues:
1. Check the logs for detailed error messages
2. Verify your API key is working at [huggingface.co](https://huggingface.co)
3. Test with different models using the comparison endpoint
4. Check the model availability at [huggingface.co/models](https://huggingface.co/models)
