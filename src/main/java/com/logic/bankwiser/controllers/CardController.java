package com.logic.bankwiser.controllers;

import com.logic.bankwiser.accounts.UserAccount;
import com.logic.bankwiser.bank_accounts.BankAccount;
import com.logic.bankwiser.cards.CreditCard;
import com.logic.bankwiser.cards.DebitCard;
import com.logic.bankwiser.storage.Storage;
import javafx.util.Pair;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;


/**
 * Controller class responsible for bank cards.
 *
 * @author Burak Askan
 * @author Mathias Hallander
 */
public class CardController {

    private final Storage storage;
    private final TransactionController TRANSACTION_CONTROLLER;
    private final String ln = System.lineSeparator();

    public CardController(Storage storage, TransactionController transactionController) {
        this.storage = storage;
        this.TRANSACTION_CONTROLLER = transactionController;
    }

    /**
     * Adding a credit card into list in storage
     *
     * @param pin             pin password for cards set by the user.
     * @param monthlyIncome   income of the user to assess size of credit.
     * @param monthlyExpenses expenses of the user to assess size of credit.
     * @return affirmative or negative string.
     */
    public String addCard(BankAccount bankAccount, int pin, int confirmPin, double monthlyIncome, double monthlyExpenses) {
        BigDecimal maxCredit;
        Pair<Boolean, BigDecimal> creditAssessment = calculateCredit(monthlyIncome, monthlyExpenses);
        if (pin != confirmPin) {
            return "PIN codes must match";
        }
        if (creditAssessment.getKey()) {
            maxCredit = creditAssessment.getValue();
        } else {
            return "Your application for a credit card has been denied.";
        }

        Pair<Boolean, String> keyAcceptance = createPasswordCheck(pin);
        if (keyAcceptance.getKey()) {
            CreditCard creditCard = new CreditCard(bankAccount, pin, maxCredit);
            bankAccount.addCard(creditCard);
            try {
                storage.storeCreditCards(creditCard);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return "Your application for a credit card has been submitted. We’ll let you know whether it has been accepted or rejected after evaluation.";
        } else {
            return keyAcceptance.getValue();
        }
    }

    /**
     * adding a debit card into list in storage
     *
     * @param pin the pin code for the card as written by user
     * @return affirmative or negative string
     */
    public String addCard(BankAccount bankAccount, int pin, int confirmPin) {
        Pair<Boolean, String> keyAcceptance = createPasswordCheck(pin);
        if (pin != confirmPin) {
            return "PIN codes must match";
        }
        if (keyAcceptance.getKey()) {
            DebitCard debitCard = new DebitCard(bankAccount, pin);
            bankAccount.addCard(debitCard);
            try {
                storage.storeDebitCards(debitCard);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return "Your application for a debit card has been accepted. We’ll let you know when it will be shipped soon.";
        } else {
            return keyAcceptance.getValue();
        }
    }

    /**
     * adding a debit card into list in storage
     *
     * @param pin the pin code for the card as written by user
     * @return affirmative or negative string
     */
    public String addCard(BankAccount bankAccount, String cardNumber, int pin, int confirmPin) {
        Pair<Boolean, String> keyAcceptance = createPasswordCheck(pin);
        if (pin != confirmPin) {
            return "PIN codes must match";
        }
        if (keyAcceptance.getKey()) {
            DebitCard debitCard = new DebitCard(bankAccount, cardNumber, pin);
            bankAccount.addCard(debitCard);
            try {
                storage.storeDebitCards(debitCard);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return "Your application for a debit card has been accepted. We’ll let you know when it will be shipped soon.";
        } else {
            return keyAcceptance.getValue();
        }
    }


    /**
     * Assess eligibility of the user and calculation of max credit (creditCards).
     *
     * @param monthlyIncome   Total income, after tax, the user has
     * @param monthlyExpenses Total fixed monthly payments and expenses that the user has
     * @return A boolean if they are eligible for a creditCard, a BigDecimal for credit value
     */
    public Pair<Boolean, BigDecimal> calculateCredit(double monthlyIncome, double monthlyExpenses) {
        boolean eligible = false;
        BigDecimal maxCredit = new BigDecimal(0);
        double incomeRemainder = monthlyIncome - monthlyExpenses;
        if (incomeRemainder > 2000) {
            eligible = true;
            maxCredit = new BigDecimal(incomeRemainder * 0.5);
        }
        return new Pair<>(eligible, maxCredit);
    }

    /**
     * Checks if given pin is legal
     *
     * @param pin pin selected by the user
     * @return affirmative or negative string and boolean
     */
    public Pair<Boolean, String> createPasswordCheck(int pin) {
        String pinString = String.valueOf(pin);
        String failCause = "";

        if (String.valueOf(pin).length() != 4) {
            return new Pair<>(false, "Invalid PIN: PIN must be four digits.");
        }

        int pinOne = Integer.parseInt(String.valueOf(pinString.charAt(0)));
        int pinTwo = Integer.parseInt(String.valueOf(pinString.charAt(1)));
        int pinThree = Integer.parseInt(String.valueOf(pinString.charAt(2)));
        int pinFour = Integer.parseInt(String.valueOf(pinString.charAt(3)));

        if (pin < 0) {
            return new Pair<>(false, "Invalid PIN: PIN code cannot be negative.");
        }

        int duplicateNumberCounter = 0;
        for (int i = 0; i < 4; i++) {
            for (int y = 0; y < 4; y++) {
                if (i != y) {
                    if (Integer.parseInt(String.valueOf(pinString.charAt(i))) == Integer.parseInt(String.valueOf(pinString.charAt(y)))) {
                        duplicateNumberCounter++;
                        if (duplicateNumberCounter == 2) {
                            return new Pair<>(false, "Invalid PIN: A number cannot be repeated more than once.");
                        }
                    }
                }
            }
        }

        if (pinOne - pinTwo == -1 || pinOne - pinTwo == 1) {
            return new Pair<>(false, "Invalid PIN: Your PIN cannot consist of numbers in consecutive order.");
        }
        if (pinThree - pinTwo == -1 || pinThree - pinTwo == 1) {
            return new Pair<>(false, "Invalid PIN: Your PIN cannot consist of numbers in consecutive order.");
        }
        if (pinThree - pinFour == -1 || pinThree - pinFour == 1) {
            return new Pair<>(false, "Invalid PIN: Your PIN cannot consist of numbers in consecutive order.");
        }
        return new Pair<>(true, failCause);
    }


    /**
     * Monthly credit card payments
     * Checks if payment due on any credit card. If there is then it does the payments.
     */
    public String creditCardPayment(UserAccount activeUser) {

        for (String bankAccountID : activeUser.getBankAccountList()) {
            BankAccount bankAccount = storage.getBankAccount(bankAccountID);

            if (bankAccount.getBalance().intValue() < 0) {
                for (DebitCard card : bankAccount.getCardMap().values()) {
                    CreditCard creditCard = (CreditCard) card;
                    LocalDateTime monthlyPaymentDate = creditCard.getMonthlyPaymentDate();
                    if (ChronoUnit.DAYS.between(monthlyPaymentDate.plusMonths(1), monthlyPaymentDate) == 0 ||
                            ChronoUnit.DAYS.between(monthlyPaymentDate.plusMonths(1), monthlyPaymentDate) < 0) {

                        BigDecimal moneyTransferred = bankAccount.getBalance().multiply(BigDecimal.valueOf(creditCard.getInterest() * -1));
                        if (moneyTransferred.compareTo(bankAccount.getBalance()) > 0) {
                            return "You are unable to pay the credit. Interests has incurred on the credit." + ln;
                        }
                        String paymentNote = "Credit payment on your credit-card number: " + card.getCardNumber();
                        TRANSACTION_CONTROLLER.transferMoney(bankAccount.getBankAccountID(), "0", moneyTransferred, paymentNote, LocalDateTime.now());

                        creditCard.setMonthlyPaymentDate(LocalDateTime.now());
                        storeCard(bankAccount, creditCard.getCardNumber());
                    }
                }
            }
        }
        return "";
    }


    /**
     * Annual card payments
     * Checks if annual payments are due on any cards. If there is then it does the payments.
     */
    public String annualCardPayment(UserAccount activeUser) {

        for (String bankAccountID : activeUser.getBankAccountList()) {
            BankAccount bankAccount = storage.getBankAccount(bankAccountID);

            for (DebitCard card : bankAccount.getCardMap().values()) {
                LocalDateTime yearlyPaymentDate = card.getYearlyPaymentDate();
                if (ChronoUnit.DAYS.between(yearlyPaymentDate.plusYears(1), yearlyPaymentDate) == 0 || ChronoUnit.DAYS.between(yearlyPaymentDate.plusYears(1), yearlyPaymentDate) < 0) {

                    String paymentNote = "Payment on your card number: " + card.getCardNumber();
                    BigDecimal moneyTransferred = new BigDecimal(999);
                    if (moneyTransferred.compareTo(bankAccount.getBalance()) > 0) {
                        return "You are unable to pay the card. You will be contacted by the bank shortly." + ln;
                    }
                    TRANSACTION_CONTROLLER.transferMoney(bankAccount.getBankAccountID(), "0", moneyTransferred, paymentNote, LocalDateTime.now());

                    card.setYearlyPaymentDate(LocalDateTime.now());
                }
            }
        }
        return "";
    }

    /**
     * Status of card being frozen or unfrozen
     *
     * @param cardNumber cardNumber of the card that will be modified
     * @return affirmative or negative string.
     */
    public String modifyStatus(BankAccount bankAccount, String cardNumber) {
        DebitCard debitCard = bankAccount.getCard(cardNumber);
        if (debitCard == null) return "Cannot toggle freezing of card: Incorrect card details.";
        if (!bankAccount.getCard(cardNumber).getFrozenStatus()) {
            bankAccount.getCard(cardNumber).setFrozenStatus(true);
            storeCard(bankAccount, cardNumber);
            return "Your card has been successfully blocked.";
        } else {
            bankAccount.getCard(cardNumber).setFrozenStatus(false);
            storeCard(bankAccount, cardNumber);
            return "Your card has been successfully unblocked.";
        }
    }

    /**
     * Changing the region in which the card can be used in
     *
     * @param cardNumber cardNumber of the card that will be modified
     * @param region     the new region that the user want the card to be locked to
     * @return affirmative or negative string.
     */
    public String modifyRegion(BankAccount bankAccount, String cardNumber, String region) {
        if (bankAccount.getCard(cardNumber) != null) {
            bankAccount.getCard(cardNumber).setRegion(region);
            storeCard(bankAccount, cardNumber);
            return "Your region has been successfully changed.";
        }
        return "Invalid input: Given card number does not exist!";
    }

    /**
     * modifying if the card will be usable for online purchases
     *
     * @param cardNumber cardNumber of the card that will be modified
     * @return affirmative or negative string.
     */
    public String modifyOnlineStatus(BankAccount bankAccount, String cardNumber) {
        if (bankAccount.getCard(cardNumber) != null) {
            if (bankAccount.getCard(cardNumber).getOnlineStatus()) {
                bankAccount.getCard(cardNumber).setOnlineStatus(false);
                storeCard(bankAccount, cardNumber);
                return "You successfully turned off online transactions.";
            } else {
                bankAccount.getCard(cardNumber).setOnlineStatus(true);
                storeCard(bankAccount, cardNumber);
                return "You successfully turned on online transactions.";
            }
        }
        return "Cannot toggle online transactions: Incorrect card details.";
    }

    /**
     * modifying the limit on how much the card can send in total
     *
     * @param cardNumber     cardNumber of the card that will be modified
     * @param expenditureMax the new maximum set on expenditure on the card
     * @return affirmative or negative string.
     */
    public String modifyExpenditureMax(BankAccount bankAccount, String cardNumber, double expenditureMax) {
        if (expenditureMax < 0) {
            return "Invalid input: The new spending limit should not be negative.";
        }
        if (bankAccount.getCard(cardNumber) != null) {
            bankAccount.getCard(cardNumber).setExpenditureMax(expenditureMax);
            double newLimit = bankAccount.getCard(cardNumber).getExpenditureMax();
            storeCard(bankAccount, cardNumber);
            return "You successfully changed your spending limit to " + newLimit + ".";
        }
        return "Invalid input: Given card number does not exist!";
    }

    /**
     * Reminds the user in how many days the card will expire
     *
     * @param cardNumber cardNumber of the card that will be reminded
     * @param user       the user that will receive the reminder
     * @return string containing reminder of expiration coming close or card has already expired
     */
    public String remainderDays(BankAccount bankAccount, String cardNumber, UserAccount user) { //Calculates remaining days until expiration

        LocalDateTime expirationDate = bankAccount.getCard(cardNumber).getExpirationDate();
        LocalDateTime dateToday = LocalDateTime.now();
        long remainderDays = ChronoUnit.DAYS.between(dateToday, expirationDate);

        if (remainderDays < 0) {
            return "The card " + cardNumber + " expired and was terminated.";
        } else if (remainderDays < 14) {
            return "Your card" + cardNumber + " will expire in " + remainderDays + " days and will be terminated then. We have sent you a new one to " + user.getAddress() + ".";
        } else {
            return "Your card" + cardNumber + " will expire in " + remainderDays + " days and will be terminated then.";
        }
    }

    /**
     * removes a card from existence within the system
     *
     * @param cardNumber           the number of the card that will get deleted
     * @param pin                  the correct pin of the card
     * @param terminationReasoning string with reasoning of termination
     * @return affirmative or negative string.
     */
    public String deleteCard(BankAccount bankAccount, String cardNumber, int pin, String terminationReasoning) {

        if (bankAccount.getCard(cardNumber) != null) {
            if (terminationReasoning.equals("")) {
                return "You have to provide at least one reason why you wish to terminate your card.";
            }
            if (pin != bankAccount.getCard(cardNumber).getPin()) {
                return "Incorrect PIN code.";
            }
            try {
                if (bankAccount.getCard(cardNumber) instanceof CreditCard) {
                    storage.storeCreditCards((CreditCard) bankAccount.getCard(cardNumber));
                } else {
                    storage.storeDebitCards(bankAccount.getCard(cardNumber));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return "Your card has been successfully terminated.";
        }
        return "Card number you entered was not found in the list of your cards.";
    }

    /**
     * changes the pin of the card
     *
     * @param cardNumber         the card that will have the pin modified
     * @param oldPin             current pin of the card
     * @param newPin             the new pin the user want to change the old pin into
     * @param newPinConfirmation same pin as newpin for confirmation
     * @return affirmative or negative string.
     */
    public String resetPin(BankAccount bankAccount, String cardNumber, int oldPin, int newPin, int newPinConfirmation) {
        if (bankAccount.getCard(cardNumber) != null) {
            if (bankAccount.getCard(cardNumber).getPin() == oldPin) {
                if (newPin == newPinConfirmation) {
                    Pair<Boolean, String> keyAcceptance = createPasswordCheck(newPin);
                    if (keyAcceptance.getKey()) {
                        bankAccount.getCard(cardNumber).setPin(newPin);
                        storeCard(bankAccount, cardNumber);
                        return "Successfully changed PIN code.";
                    } else {
                        return keyAcceptance.getValue();
                    }
                } else {
                    return "Cannot change PIN code: New PIN code doesn’t match the one in the confirmation field.";
                }
            } else {
                return "Incorrect PIN code.";
            }
        }
        return "Card number you entered was not found in the list of your cards.";
    }

    /**
     * checks if given pin is correct
     *
     * @param cardNumber Number of the card that is getting pin checked
     * @param pin        the pin that user entered with that pin
     * @return boolean if the pin entered was correct ot not
     */
    public boolean checkPin(BankAccount bankAccount, String cardNumber, int pin) {
        return (bankAccount.getCard(cardNumber).getPin() == pin);
    }

    public void storeCard(BankAccount bankAccount, String cardNumber) {
        try {
            if (bankAccount.getCard(cardNumber) instanceof CreditCard) {
                storage.storeCreditCards((CreditCard) bankAccount.getCard(cardNumber));
            } else {
                storage.storeDebitCards(bankAccount.getCard(cardNumber));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
