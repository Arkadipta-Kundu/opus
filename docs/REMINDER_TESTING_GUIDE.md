# How to Test the Email Reminder System

## 1. Setup Instructions

### Prerequisites

- Make sure Redis is running on localhost:6379
- Ensure your email configuration is working in application.properties
- Start your Opus application

### Verify Setup

1. Check if scheduling is enabled: `GET /api/admin/scheduling/status`
2. Watch the console logs for scheduled task messages

## 2. Testing Email Reminders

### Step 1: Create a Task

```bash
POST /api/tasks
{
  "taskTitle": "Test Task for Reminder",
  "taskDesc": "This is a test task to demonstrate email reminders",
  "taskStatus": "TODO"
}
```

### Step 2: Schedule a Reminder

```bash
POST /api/reminders/schedule
{
  "taskId": 1,
  "reminderTime": "2025-08-05T17:00:00",
  "message": "Don't forget to complete this test task!"
}
```

### Step 3: View Your Reminders

```bash
GET /api/reminders/my-reminders
```

### Step 4: Check Reminder Statistics

```bash
GET /api/reminders/stats
```

### Step 5: Cancel a Reminder (if needed)

```bash
DELETE /api/reminders/{reminderId}
```

## 3. Example API Calls using curl

### Schedule a reminder for 2 minutes from now:

```bash
curl -X POST http://localhost:8080/api/reminders/schedule \
  -H "Content-Type: application/json" \
  -H "Authorization: Basic $(echo -n 'username:password' | base64)" \
  -d '{
    "taskId": 1,
    "reminderTime": "2025-08-05T16:30:00",
    "message": "Time to work on your task!"
  }'
```

### Get all your reminders:

```bash
curl -X GET http://localhost:8080/api/reminders/my-reminders \
  -H "Authorization: Basic $(echo -n 'username:password' | base64)"
```

## 4. Expected Log Messages

You should see these types of messages in your console:

```
🕐 Fixed Rate Task executed at: 2025-08-05 16:25:30
📅 Cron Task (every minute) executed at: 2025-08-05 16:26:00
✅ Scheduled reminder 1 for task 1 at 2025-08-05T16:30:00
✅ Sent reminder email for task 1 to user@example.com
🧹 Cleaned up 0 old reminders
📊 Reminder Statistics - Pending: 1, Sent Today: 0
```

## 5. What Happens When Reminder Time Arrives

1. TaskScheduler triggers the email sending
2. Email is sent using your configured SMTP settings
3. Reminder status changes from PENDING to SENT
4. sentAt timestamp is recorded
5. Success message is logged

## 6. Troubleshooting

### If emails are not being sent:

- Check email configuration in application.properties
- Verify Gmail app password is correct
- Check firewall/network settings
- Look for error messages in logs

### If scheduling is not working:

- Verify @EnableScheduling is present
- Check if TaskScheduler bean is created
- Look for thread pool issues in logs

### If reminders are missed:

- The cleanup service runs every 5 minutes to catch missed reminders
- Check the "processMissedReminders" log messages

## 7. Production Considerations

### Security

- Add proper authentication/authorization
- Validate reminder times (not too far in future)
- Rate limiting for reminder creation

### Performance

- Monitor thread pool usage
- Consider using external job scheduler for high volume
- Implement proper error handling and retries

### Monitoring

- Set up alerts for failed reminders
- Monitor reminder statistics
- Log important events for auditing
