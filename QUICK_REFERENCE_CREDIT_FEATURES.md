# 🧩 Credit & Architecture Features - Quick Reference

## 🎯 What Was Implemented

### 1️⃣ **Architecture Compatibility Check**

✅ Execution buttons are **disabled** if the program contains instructions that require a higher architecture than selected.

**Example**: If your program has Architecture II instructions, but you select Architecture I, the Start buttons will be disabled and you'll see:

```
⚠ Program contains instructions requiring: Architecture II
```

---

### 2️⃣ **Architecture Cost Deduction**

✅ When you start a program, credits are deducted based on the selected architecture:

| Architecture | Cost         |
| ------------ | ------------ |
| I            | 5 credits    |
| II           | 100 credits  |
| III          | 500 credits  |
| IV           | 1000 credits |

**What happens**: Before execution starts, the system deducts the architecture cost from your account.

---

### 3️⃣ **Credit Validation Before Run**

✅ The system checks if you have enough credits before allowing execution.

**What happens**:

- If you have **less than** the architecture cost → ❌ Execution blocked
- Alert shows: "You need at least X credits... You have Y credits"
- You must load more credits to continue

---

### 4️⃣ **Per-Cycle Credit Deduction**

✅ During execution, **1 credit is deducted per cycle**

**Debug Mode**:

- Each "Step Over" button click = 1 cycle = 1 credit deducted

**Regular Mode**:

- Server automatically deducts 1 credit per cycle during execution

---

### 5️⃣ **Real-Time Credit Display**

✅ Your credit balance updates in real-time during execution

**Debug Mode**:

```
Step executed - Instruction 5 of 20 | Credits: 143
```

**Regular Mode**:

```
Running... (Cycles: 12 | Credits: 138)
```

---

### 6️⃣ **Out-of-Credits Protection**

✅ If you run out of credits during execution:

**What happens**:

1. Alert shows: "You have run out of credits!"
2. Execution stops immediately
3. You're navigated back to the Programs screen
4. You need to load more credits and try again

---

## 📊 Total Cost Calculation

**Total Cost = Architecture Cost + (Number of Cycles × 1)**

### Example 1: Simple Program on Architecture I

- Architecture Cost: **5 credits**
- Program runs for **10 cycles**: **10 credits**
- **Total**: 5 + 10 = **15 credits**

### Example 2: Complex Program on Architecture III

- Architecture Cost: **500 credits**
- Program runs for **50 cycles**: **50 credits**
- **Total**: 500 + 50 = **550 credits**

---

## 🚦 Execution Flow

```
1. Load Program ✓
   ↓
2. Select Architecture ✓
   ↓
3. System Checks Compatibility
   ├─ Compatible? → Continue
   └─ Not Compatible? → ❌ Buttons Disabled
   ↓
4. Enter Input Values ✓
   ↓
5. Click "Start Debug" or "Start Regular" ✓
   ↓
6. System Checks Credits
   ├─ Enough credits? → Continue
   └─ Not enough? → ❌ Alert & Block
   ↓
7. Deduct Architecture Cost
   ↓
8. Execution Starts
   ↓
9. For Each Cycle:
   ├─ Check Credits
   ├─ Deduct 1 Credit
   ├─ Execute Instruction
   └─ Update Display
   ↓
10. Out of Credits?
    ├─ Yes → ⚠ Stop & Alert
    └─ No → Continue
   ↓
11. Program Complete ✅
```

---

## 🎮 How to Use

### Starting Debug Execution

1. Load your program
2. Select architecture (buttons disabled if incompatible)
3. Enter input values
4. Click **"Start Debug"**
5. System validates credits
6. Architecture cost deducted
7. Use **"Step Over"** to execute one instruction at a time
8. Watch your credits decrease by 1 per step
9. Status shows: "Step executed - Instruction X of Y | Credits: Z"

### Starting Regular Execution

1. Load your program
2. Select architecture
3. Enter input values
4. Click **"Start Regular"**
5. System validates credits
6. Architecture cost deducted
7. Program runs automatically
8. Status shows: "Running... (Cycles: X | Credits: Y)"
9. Credits update every 500ms
10. Program completes or stops if credits run out

---

## ⚠️ Common Scenarios

### Scenario: "Start buttons are grayed out"

**Cause**: Your program has instructions that require a higher architecture.

**Solution**:

1. Look at the status message
2. It will say: "⚠ Program contains instructions requiring: Architecture X"
3. Select a higher architecture from the dropdown

---

### Scenario: "Can't start execution - insufficient credits"

**Cause**: You don't have enough credits for the architecture cost.

**Solution**:

1. Check the alert message
2. It shows how many credits you need vs. how many you have
3. Go back to dashboard
4. Click "Load Credits" button
5. Load enough credits
6. Try again

---

### Scenario: "Execution stopped mid-run"

**Cause**: You ran out of credits during execution.

**Solution**:

1. Alert shows: "You have run out of credits!"
2. You're taken back to Programs screen
3. Load more credits
4. Re-run the program with the same inputs

---

## 💡 Tips

1. **Check Architecture Requirements**: Look at the architecture summary line at the bottom of the instruction table. Red numbers indicate unsupported architectures.

2. **Estimate Costs**: Before running, check:

   - Architecture cost (displayed in dropdown)
   - Number of instructions (visible in table)
   - Multiply instructions × expected loops to estimate cycles

3. **Use Debug Mode for Testing**: Debug mode lets you control execution step-by-step and see exactly how many cycles your program needs.

4. **Load Extra Credits**: Always keep some buffer credits. Programs can run for more cycles than expected.

5. **Monitor Credit Display**: Watch the real-time credit counter during execution to know when to stop.

---

## 🔧 Troubleshooting

| Issue                            | Solution                                            |
| -------------------------------- | --------------------------------------------------- |
| Buttons disabled                 | Check architecture compatibility message            |
| "Insufficient credits" alert     | Load more credits from dashboard                    |
| Execution stops unexpectedly     | Likely ran out of credits - check alert             |
| Credits not updating             | Refresh the page or check network connection        |
| Can't select higher architecture | That architecture may not support your instructions |

---

## 📞 Key Status Messages

| Message                                                     | Meaning                             |
| ----------------------------------------------------------- | ----------------------------------- |
| `⚠ Program contains instructions requiring: Architecture X` | Selected architecture is too low    |
| `Ready to execute`                                          | All checks passed, ready to run     |
| `Deducting architecture cost (X credits)...`                | Architecture cost being deducted    |
| `Step executed - Instruction X of Y \| Credits: Z`          | Debug step completed, credits shown |
| `Running... (Cycles: X \| Credits: Y)`                      | Regular execution in progress       |
| `You have run out of credits!`                              | No credits left, execution stopped  |

---

**Quick Reference v1.0** | October 13, 2025
