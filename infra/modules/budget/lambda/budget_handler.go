package main

import (
	"context"
	"encoding/json"
	"fmt"
	"log"
	"os"

	"github.com/aws/aws-lambda-go/events"
	"github.com/aws/aws-lambda-go/lambda"
	"github.com/aws/aws-sdk-go-v2/aws"
	"github.com/aws/aws-sdk-go-v2/config"
	"github.com/aws/aws-sdk-go-v2/service/ses"
	"github.com/aws/aws-sdk-go-v2/service/ses/types"
)

// BudgetNotification represents the payload published by AWS Budgets via SNS.
type BudgetNotification struct {
	AccountID        string      `json:"accountId"`
	BudgetName       string      `json:"budgetName"`
	BudgetType       string      `json:"budgetType"`
	NotificationType string      `json:"notificationType"` // ACTUAL or FORECASTED
	ThresholdExceeded float64    `json:"thresholdExceeded"`
	ActualSpend      SpendAmount `json:"actualSpend"`
	BudgetedAmount   SpendAmount `json:"budgetedAmount"`
}

// SpendAmount holds an amount string and its currency unit.
type SpendAmount struct {
	Amount string `json:"amount"`
	Unit   string `json:"unit"`
}

// handler processes incoming SNS events triggered by AWS Budgets.
func handler(ctx context.Context, snsEvent events.SNSEvent) error {
	alertEmail  := os.Getenv("ALERT_EMAIL")
	senderEmail := os.Getenv("SENDER_EMAIL")
	environment := os.Getenv("ENVIRONMENT")
	if environment == "" {
		environment = "unknown"
	}

	if alertEmail == "" || senderEmail == "" {
		log.Println("ALERT_EMAIL or SENDER_EMAIL is not configured; skipping SES dispatch")
		return nil
	}

	cfg, err := config.LoadDefaultConfig(ctx)
	if err != nil {
		return fmt.Errorf("load AWS config: %w", err)
	}
	sesClient := ses.NewFromConfig(cfg)

	for _, record := range snsEvent.Records {
		rawMessage := record.SNS.Message
		log.Printf("Received raw SNS message: %s", rawMessage)

		var notification BudgetNotification
		if err := json.Unmarshal([]byte(rawMessage), &notification); err != nil {
			// Log and continue — do not fail the entire batch for one bad message.
			log.Printf("Failed to unmarshal JSON payload: %v | raw: %s", err, rawMessage)
			continue
		}

		subject := formatSubject(environment, notification)
		body    := formatBody(environment, notification)

		log.Printf("Sending budget alert email: %s", subject)

		if err := sendEmail(ctx, sesClient, senderEmail, alertEmail, subject, body); err != nil {
			log.Printf("Failed to send SES email: %v", err)
		}
	}

	return nil
}

// formatSubject returns a short email subject line for the alert.
func formatSubject(env string, n BudgetNotification) string {
	return fmt.Sprintf("[SpendSync/%s] Budget Alert: %s exceeded %.0f%%",
		env, n.BudgetName, n.ThresholdExceeded)
}

// formatBody returns a plain-text email body with full alert details.
func formatBody(env string, n BudgetNotification) string {
	return fmt.Sprintf(
		"SpendSync Budget Alert\n"+
			"======================\n\n"+
			"Environment     : %s\n"+
			"Account ID      : %s\n"+
			"Budget Name     : %s\n"+
			"Budget Type     : %s\n"+
			"Notification    : %s\n"+
			"Threshold       : %.0f%% exceeded\n"+
			"Actual Spend    : %s %s\n"+
			"Budgeted Amount : %s %s\n\n"+
			"Please review your AWS Cost Explorer for details.\n"+
			"https://console.aws.amazon.com/cost-management/home#/cost-explorer",
		env,
		n.AccountID,
		n.BudgetName,
		n.BudgetType,
		n.NotificationType,
		n.ThresholdExceeded,
		n.ActualSpend.Amount, n.ActualSpend.Unit,
		n.BudgetedAmount.Amount, n.BudgetedAmount.Unit,
	)
}

// sendEmail dispatches a plain-text email via Amazon SES.
func sendEmail(ctx context.Context, client *ses.Client, from, to, subject, body string) error {
	input := &ses.SendEmailInput{
		Source: aws.String(from),
		Destination: &types.Destination{
			ToAddresses: []string{to},
		},
		Message: &types.Message{
			Subject: &types.Content{
				Data:    aws.String(subject),
				Charset: aws.String("UTF-8"),
			},
			Body: &types.Body{
				Text: &types.Content{
					Data:    aws.String(body),
					Charset: aws.String("UTF-8"),
				},
			},
		},
	}

	out, err := client.SendEmail(ctx, input)
	if err != nil {
		return fmt.Errorf("ses SendEmail: %w", err)
	}

	log.Printf("SES email sent successfully (MessageId: %s)", aws.ToString(out.MessageId))
	return nil
}

func main() {
	lambda.Start(handler)
}
