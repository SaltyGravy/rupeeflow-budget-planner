/**
 * RupeeFlow - Personal Finance & Budget Planner
 * Pure JavaScript Client for Spring Boot Backend with Standalone Vercel Mode
 */

// Category Definitions
const INCOME_CATEGORIES = ["Salary", "Pocket money", "Freelancing", "Other income"];
const EXPENSE_CATEGORIES = ["Food", "Travel", "Shopping", "Education", "Bills", "Entertainment", "Other"];

const PIE_COLORS = [
  "#10b981", // Emerald
  "#6366f1", // Indigo
  "#a855f7", // Purple
  "#f43f5e", // Rose
  "#f59e0b", // Amber
  "#06b6d4", // Cyan
  "#ec4899", // Pink
  "#84cc16"  // Lime
];

// State
let currentUser = null;
let currentView = "dashboard";
let selectedModalType = "EXPENSE";
let chartInstances = {};
let isClientStorageMode = false;

// Helper: Format Currency in INR (₹)
function formatRupees(amount) {
  const num = Number(amount);
  if (isNaN(num)) return "₹0";
  return new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    maximumFractionDigits: 0
  }).format(num);
}

// Helper: Format Date
function formatDate(dateStr) {
  if (!dateStr) return "";
  const d = new Date(dateStr + "T00:00:00");
  return d.toLocaleDateString("en-IN", {
    day: "numeric",
    month: "short",
    year: "numeric"
  });
}

// Helper: Toast Notifications
function showToast(message, type = "success") {
  const container = document.getElementById("toastContainer");
  const toast = document.createElement("div");
  toast.className = `toast ${type}`;
  toast.innerHTML = `
    <span>${type === "success" ? "✓" : "⚠️"}</span>
    <span>${message}</span>
  `;
  container.appendChild(toast);
  setTimeout(() => {
    toast.style.opacity = "0";
    toast.style.transform = "translateY(10px)";
    toast.style.transition = "all 0.25s ease";
    setTimeout(() => toast.remove(), 250);
  }, 3500);
}

// ===================================================================
// Standalone Client Data Store (for Vercel & Offline Deployments)
// ===================================================================
const ClientStore = {
  get(key, def) {
    try {
      const data = localStorage.getItem("rupeeflow_" + key);
      return data ? JSON.parse(data) : def;
    } catch {
      return def;
    }
  },
  set(key, val) {
    try {
      localStorage.setItem("rupeeflow_" + key, JSON.stringify(val));
    } catch (e) {
      console.warn("localStorage quota exceeded", e);
    }
  },
  /**
   * Builds the sample Demo Account (Aarav Kumar) with facade data.
   *
   * All the numbers here are made-up placeholder values, used only so the
   * dashboard, charts and reports look realistic when someone tries the demo.
   *
   * Two rules keep the demo believable:
   *   1. Nothing is ever dated in the future (you cannot spend money tomorrow).
   *   2. Every month has slightly different values, so the 6-month chart is not flat.
   *
   * These numbers mirror DemoDataLoader.java on the Spring Boot backend.
   */
  init() {
    // Bump SEED_VERSION whenever the demo numbers below change, so returning
    // visitors get the refreshed data instead of the old copy in localStorage.
    const SEED_VERSION = 2;
    if (this.get("seed_version", 0) === SEED_VERSION) return;

    const now = new Date();
    const todayDay = now.getDate();
    const monthKeyOf = (d) => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}`;
    const thisMonth = monthKeyOf(now);

    const users = [
      { id: 1, email: "demo@rupeeflow.com", password: "password123", fullName: "Aarav Kumar" }
    ];

    // Target is kept well above the demo's saved-up amount so the progress
    // bar shows real partial progress (~65%) instead of sitting at 100%.
    const goals = [
      { userId: 1, title: "Emergency Fund & MacBook", targetAmount: 500000 }
    ];

    const budgets = [];
    const transactions = [];
    let nextId = 1;

    const add = (type, category, amount, occurredOn, description) => {
      // Skip empty amounts so an unused category never shows as a ₹0 row
      if (!amount || amount <= 0) return;
      transactions.push({ id: nextId++, userId: 1, type, category, amount, occurredOn, description });
    };

    const pad = (n) => String(n).padStart(2, "0");

    // =================================================================
    // 1. FIVE PAST MONTHS (gives the 6-month chart a varied history)
    // =================================================================
    // Column order:
    // salary, extraIncome, food, bills, travel, shopping, fun, education, misc, budget
    const pastMonths = [
      [72000, 2000, 7200, 4100, 2800, 5400, 2100, 1800, 900, 38000],  // 5 months ago - steady
      [72000, 8000, 8100, 4600, 3300, 7200, 3100, 2400, 0, 38000],    // 4 months ago - freelance project
      [75000, 0, 6900, 5200, 2600, 4100, 1900, 0, 1200, 40000],       // 3 months ago - quiet month
      [75000, 15500, 7800, 4300, 4100, 9600, 3600, 2000, 0, 35000],   // 2 months ago - festival overspend (fires 80% alert)
      [78000, 1500, 7400, 4400, 2900, 5100, 2300, 2600, 1100, 42000]  // last month    - back to normal
    ];

    // Where the extra income in each month came from
    const extraIncomeCategory = ["Pocket money", "Freelancing", "", "Freelancing", "Pocket money"];
    const extraIncomeNote = [
      "Gift from family", "Freelance website project", "",
      "Festive freelance bonus", "Gift from family"
    ];

    pastMonths.forEach((row, i) => {
      // Row 0 is the oldest month, so it goes back the furthest
      const monthsAgo = pastMonths.length - i;
      const monthDate = new Date(now.getFullYear(), now.getMonth() - monthsAgo, 1);
      const mKey = monthKeyOf(monthDate);
      const on = (day) => `${mKey}-${pad(day)}`;

      // Monthly budget the user had set for that month
      budgets.push({ userId: 1, month: mKey, amount: row[9] });

      // Income
      add("INCOME", "Salary", row[0], on(1), "Monthly salary - Tech Corp");
      if (row[1] > 0) {
        add("INCOME", extraIncomeCategory[i], row[1], on(6), extraIncomeNote[i]);
      }

      // Expenses (a zero amount simply means that category was unused that month)
      add("EXPENSE", "Food", row[2], on(4), "Groceries & dining");
      add("EXPENSE", "Bills", row[3], on(7), "Electricity, water & broadband");
      add("EXPENSE", "Travel", row[4], on(11), "Fuel, metro & cabs");
      add("EXPENSE", "Shopping", row[5], on(15), "Clothing & household");
      add("EXPENSE", "Entertainment", row[6], on(19), "Movies & eating out");
      add("EXPENSE", "Education", row[7], on(23), "Courses & books");
      add("EXPENSE", "Other", row[8], on(26), "Gifts & miscellaneous");
    });

    // =================================================================
    // 2. CURRENT MONTH (day by day, stopping at today)
    // =================================================================
    budgets.push({ userId: 1, month: thisMonth, amount: 42000 });

    // Adds a transaction for a day of THIS month, but only if that day has
    // already happened. This keeps the demo free of future-dated money.
    const addIfNotFuture = (type, category, amount, day, description) => {
      if (day > todayDay) return;
      add(type, category, amount, `${thisMonth}-${pad(day)}`, description);
    };

    // Income for this month
    addIfNotFuture("INCOME", "Salary", 78000, 1, "Monthly salary - Tech Corp");
    addIfNotFuture("INCOME", "Freelancing", 14500, 6, "Freelance project milestone");
    addIfNotFuture("INCOME", "Other income", 2400, 17, "Cashback & interest credited");

    // Expenses for this month, spread across the days of the month.
    // Bigger fixed costs sit on the first few days, so the dashboard still
    // looks active even when today is early in the month.
    addIfNotFuture("EXPENSE", "Bills", 4450, 1, "Electricity & broadband bill");
    addIfNotFuture("EXPENSE", "Food", 2850, 2, "Monthly grocery run");
    addIfNotFuture("EXPENSE", "Travel", 780, 3, "Metro card recharge");
    addIfNotFuture("EXPENSE", "Food", 1240, 5, "Weekend dinner out");
    addIfNotFuture("EXPENSE", "Shopping", 3300, 6, "Running shoes");
    addIfNotFuture("EXPENSE", "Entertainment", 920, 8, "Movie tickets");
    addIfNotFuture("EXPENSE", "Food", 1680, 10, "Groceries top-up");
    addIfNotFuture("EXPENSE", "Travel", 1450, 12, "Cab rides & fuel");
    addIfNotFuture("EXPENSE", "Education", 2200, 14, "Online course subscription");
    addIfNotFuture("EXPENSE", "Shopping", 2750, 16, "Home essentials");
    addIfNotFuture("EXPENSE", "Food", 1520, 18, "Cafe & takeaway");
    addIfNotFuture("EXPENSE", "Entertainment", 1350, 20, "Concert tickets");
    addIfNotFuture("EXPENSE", "Bills", 1180, 22, "Mobile & OTT subscriptions");
    addIfNotFuture("EXPENSE", "Travel", 860, 24, "Auto & metro");
    addIfNotFuture("EXPENSE", "Food", 1960, 26, "Groceries");
    addIfNotFuture("EXPENSE", "Other", 1100, 28, "Gifts & miscellaneous");

    this.set("users", users);
    this.set("budgets", budgets);
    this.set("goals", goals);
    this.set("transactions", transactions);
    this.set("initialized", true);
    this.set("seed_version", SEED_VERSION);
  }
};

// Unified API Service
const api = {
  async getAuth() {
    if (!isClientStorageMode) {
      try {
        const res = await fetch("/api/auth/me");
        if (res.status === 404) {
          // Running on static server / Vercel without active backend proxy
          isClientStorageMode = true;
          ClientStore.init();
          return this.getAuth();
        }
        if (res.ok) return await res.json();
        return null;
      } catch {
        isClientStorageMode = true;
        ClientStore.init();
        return this.getAuth();
      }
    }
    const currentId = ClientStore.get("current_session_user", null);
    if (!currentId) return null;
    const users = ClientStore.get("users", []);
    const found = users.find(u => u.id === currentId);
    return found ? { id: found.id, email: found.email, fullName: found.fullName } : null;
  },

  async login(email, password) {
    if (!isClientStorageMode) {
      const res = await fetch("/api/auth/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email, password })
      });
      const data = await res.json();
      if (!res.ok) throw new Error(data.error || "Login failed");
      return data;
    }
    const users = ClientStore.get("users", []);
    const user = users.find(u => u.email.toLowerCase() === email.toLowerCase() && u.password === password);
    if (!user) throw new Error("Invalid email or password.");
    ClientStore.set("current_session_user", user.id);
    return { id: user.id, email: user.email, fullName: user.fullName };
  },

  async register(fullName, email, password) {
    if (!isClientStorageMode) {
      const res = await fetch("/api/auth/register", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ fullName, email, password })
      });
      const data = await res.json();
      if (!res.ok) throw new Error(data.error || "Registration failed");
      return data;
    }
    const users = ClientStore.get("users", []);
    if (users.some(u => u.email.toLowerCase() === email.toLowerCase())) {
      throw new Error("An account with this email already exists.");
    }
    const newUser = { id: Date.now(), fullName, email, password };
    users.push(newUser);
    ClientStore.set("users", users);
    ClientStore.set("current_session_user", newUser.id);
    return { id: newUser.id, email: newUser.email, fullName: newUser.fullName };
  },

  async logout() {
    if (!isClientStorageMode) {
      try { await fetch("/api/auth/logout", { method: "POST" }); } catch {}
    } else {
      ClientStore.set("current_session_user", null);
    }
  },

  async getDashboardStats(month) {
    if (!isClientStorageMode) {
      const res = await fetch(month ? `/api/dashboard/stats?month=${month}` : "/api/dashboard/stats");
      if (!res.ok) throw new Error("Failed to load dashboard stats");
      return await res.json();
    }

    const now = new Date();
    const targetMonth = month || `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}`;
    const [y, m] = targetMonth.split("-");
    const d = new Date(Number(y), Number(m) - 1, 1);
    const monthLabel = d.toLocaleDateString("en-IN", { month: "short", year: "numeric" });

    const allTx = (ClientStore.get("transactions", [])).filter(t => t.userId === currentUser.id);
    const monthTx = allTx.filter(t => t.occurredOn.startsWith(targetMonth));

    const totalIncome = monthTx.filter(t => t.type === "INCOME").reduce((s, t) => s + Number(t.amount), 0);
    const totalExpense = monthTx.filter(t => t.type === "EXPENSE").reduce((s, t) => s + Number(t.amount), 0);
    const remainingBalance = totalIncome - totalExpense;
    const savingsThisMonth = Math.max(remainingBalance, 0);

    const allIncome = allTx.filter(t => t.type === "INCOME").reduce((s, t) => s + Number(t.amount), 0);
    const allExpense = allTx.filter(t => t.type === "EXPENSE").reduce((s, t) => s + Number(t.amount), 0);
    const savingsTotalAllTime = Math.max(allIncome - allExpense, 0);

    const budgets = ClientStore.get("budgets", []);
    const budgetObj = budgets.find(b => b.userId === currentUser.id && b.month === targetMonth);
    const monthlyBudget = budgetObj ? Number(budgetObj.amount) : 0;
    const budgetRemainingAmount = Math.max(monthlyBudget - totalExpense, 0);
    const budgetUsedPercentage = monthlyBudget > 0 ? (totalExpense / monthlyBudget) * 100 : 0;
    const budgetAlert = monthlyBudget > 0 && budgetUsedPercentage >= 80;
    const budgetAlertMessage = budgetAlert
      ? `You have used ${Math.round(budgetUsedPercentage)}% of your monthly budget. ${formatRupees(budgetRemainingAmount)} remaining.`
      : "";

    const goals = ClientStore.get("goals", []);
    const goal = goals.find(g => g.userId === currentUser.id);
    const savingsGoalTitle = goal ? goal.title : "Savings goal";
    const savingsGoalTarget = goal ? Number(goal.targetAmount) : 0;
    const savingsGoalPercentage = savingsGoalTarget > 0 ? Math.min(100, (savingsTotalAllTime / savingsGoalTarget) * 100) : 0;

    // Categories
    const catMap = {};
    monthTx.filter(t => t.type === "EXPENSE").forEach(t => {
      catMap[t.category] = (catMap[t.category] || 0) + Number(t.amount);
    });
    const categoryBreakdown = Object.entries(catMap)
      .sort((a, b) => b[1] - a[1])
      .map(([category, amount]) => ({
        category,
        amount,
        percentage: totalExpense > 0 ? (amount / totalExpense) * 100 : 0
      }));

    // 6-month trend
    const sixMonthTrend = [];
    for (let i = 5; i >= 0; i--) {
      const prevDate = new Date(Number(y), Number(m) - 1 - i, 1);
      const prevKey = `${prevDate.getFullYear()}-${String(prevDate.getMonth() + 1).padStart(2, "0")}`;
      const prevLabel = prevDate.toLocaleDateString("en-IN", { month: "short" });

      const txs = allTx.filter(t => t.occurredOn.startsWith(prevKey));
      const inc = txs.filter(t => t.type === "INCOME").reduce((s, t) => s + Number(t.amount), 0);
      const exp = txs.filter(t => t.type === "EXPENSE").reduce((s, t) => s + Number(t.amount), 0);
      sixMonthTrend.push({ month: prevKey, monthLabel: prevLabel, income: inc, expense: exp });
    }

    // Recent 5
    const recentTransactions = [...allTx].sort((a, b) => b.occurredOn.localeCompare(a.occurredOn)).slice(0, 5);

    return {
      month: targetMonth,
      monthLabel,
      totalIncome,
      totalExpense,
      remainingBalance,
      savingsThisMonth,
      savingsTotalAllTime,
      monthlyBudget,
      budgetUsedAmount: totalExpense,
      budgetRemainingAmount,
      budgetUsedPercentage,
      budgetAlert,
      budgetAlertMessage,
      savingsGoalTitle,
      savingsGoalTarget,
      savingsGoalPercentage,
      categoryBreakdown,
      sixMonthTrend,
      recentTransactions
    };
  },

  async getTransactions(params) {
    if (!isClientStorageMode) {
      const res = await fetch(`/api/transactions?${params.toString()}`);
      if (!res.ok) throw new Error("Could not fetch transactions");
      return await res.json();
    }

    let txs = (ClientStore.get("transactions", [])).filter(t => t.userId === currentUser.id);
    const type = params.get("type");
    const category = params.get("category");
    const from = params.get("from");
    const to = params.get("to");
    const search = (params.get("search") || "").trim().toLowerCase();

    return txs.filter(t => {
      if (type && t.type !== type) return false;
      if (category && t.category !== category) return false;
      if (from && t.occurredOn < from) return false;
      if (to && t.occurredOn > to) return false;
      if (search) {
        const text = `${t.description || ""} ${t.category} ${t.amount}`.toLowerCase();
        if (!text.includes(search)) return false;
      }
      return true;
    }).sort((a, b) => b.occurredOn.localeCompare(a.occurredOn));
  },

  async saveTransaction(id, data) {
    if (!isClientStorageMode) {
      const url = id ? `/api/transactions/${id}` : "/api/transactions";
      const method = id ? "PUT" : "POST";
      const res = await fetch(url, {
        method,
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(data)
      });
      const resData = await res.json();
      if (!res.ok) throw new Error(resData.error || "Save failed");
      return resData;
    }

    const txs = ClientStore.get("transactions", []);
    if (id) {
      const idx = txs.findIndex(t => t.id === Number(id) && t.userId === currentUser.id);
      if (idx !== -1) {
        txs[idx] = { ...txs[idx], ...data };
      }
    } else {
      txs.push({
        id: Date.now(),
        userId: currentUser.id,
        ...data,
        createdAt: new Date().toISOString()
      });
    }
    ClientStore.set("transactions", txs);
    return { success: true };
  },

  async deleteTransaction(id) {
    if (!isClientStorageMode) {
      const res = await fetch(`/api/transactions/${id}`, { method: "DELETE" });
      if (!res.ok) throw new Error("Could not delete");
      return await res.json();
    }
    let txs = ClientStore.get("transactions", []);
    txs = txs.filter(t => t.id !== Number(id) || t.userId !== currentUser.id);
    ClientStore.set("transactions", txs);
    return { success: true };
  },

  async getBudget() {
    if (!isClientStorageMode) {
      const res = await fetch("/api/budgets/current");
      return await res.json();
    }
    const now = new Date();
    const thisMonth = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}`;
    const budgets = ClientStore.get("budgets", []);
    const b = budgets.find(item => item.userId === currentUser.id && item.month === thisMonth);
    return { month: thisMonth, amount: b ? b.amount : 0 };
  },

  async saveBudget(month, amount) {
    if (!isClientStorageMode) {
      const res = await fetch("/api/budgets", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ month, amount })
      });
      return await res.json();
    }
    const budgets = ClientStore.get("budgets", []);
    const idx = budgets.findIndex(b => b.userId === currentUser.id && b.month === month);
    if (idx !== -1) budgets[idx].amount = amount;
    else budgets.push({ userId: currentUser.id, month, amount });
    ClientStore.set("budgets", budgets);
    return { month, amount };
  },

  async getSavingsGoal() {
    if (!isClientStorageMode) {
      const res = await fetch("/api/savings-goals");
      return await res.json();
    }
    const goals = ClientStore.get("goals", []);
    const g = goals.find(item => item.userId === currentUser.id);
    return g ? { title: g.title, targetAmount: g.targetAmount } : { title: "Savings goal", targetAmount: 0 };
  },

  async saveSavingsGoal(title, targetAmount) {
    if (!isClientStorageMode) {
      const res = await fetch("/api/savings-goals", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ title, targetAmount })
      });
      return await res.json();
    }
    const goals = ClientStore.get("goals", []);
    const idx = goals.findIndex(g => g.userId === currentUser.id);
    if (idx !== -1) {
      goals[idx] = { ...goals[idx], title, targetAmount };
    } else {
      goals.push({ userId: currentUser.id, title, targetAmount });
    }
    ClientStore.set("goals", goals);
    return { title, targetAmount };
  },

  async getReports(month) {
    if (!isClientStorageMode) {
      const url = month ? `/api/reports?month=${month}` : "/api/reports";
      const res = await fetch(url);
      return await res.json();
    }

    const now = new Date();
    const targetMonth = month || `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}`;
    const [y, m] = targetMonth.split("-");
    const d = new Date(Number(y), Number(m) - 1, 1);
    const monthLabel = d.toLocaleDateString("en-IN", { month: "short", year: "numeric" });

    const allTx = (ClientStore.get("transactions", [])).filter(t => t.userId === currentUser.id);
    const monthTx = allTx.filter(t => t.occurredOn.startsWith(targetMonth));

    const totalIncome = monthTx.filter(t => t.type === "INCOME").reduce((s, t) => s + Number(t.amount), 0);
    const totalExpense = monthTx.filter(t => t.type === "EXPENSE").reduce((s, t) => s + Number(t.amount), 0);
    const remainingBalance = totalIncome - totalExpense;
    const totalSavings = Math.max(remainingBalance, 0);

    const catMap = {};
    monthTx.filter(t => t.type === "EXPENSE").forEach(t => {
      catMap[t.category] = (catMap[t.category] || 0) + Number(t.amount);
    });
    const categoryBreakdown = Object.entries(catMap)
      .sort((a, b) => b[1] - a[1])
      .map(([category, amount]) => ({
        category,
        amount,
        percentage: totalExpense > 0 ? (amount / totalExpense) * 100 : 0
      }));

    const highest = categoryBreakdown[0];
    const highestSpendingCategory = highest ? highest.category : "—";
    const highestSpendingCategoryAmount = highest ? highest.amount : 0;

    const budgets = ClientStore.get("budgets", []);
    const b = budgets.find(item => item.userId === currentUser.id && item.month === targetMonth);
    const budgetAmount = b ? Number(b.amount) : 0;
    const budgetRemaining = Math.max(budgetAmount - totalExpense, 0);

    const twelveMonthTrend = [];
    const availableMonths = [];
    for (let i = 11; i >= 0; i--) {
      const date = new Date(now.getFullYear(), now.getMonth() - i, 1);
      const key = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}`;
      const label = date.toLocaleDateString("en-IN", { month: "short", year: "numeric" });
      const exp = allTx.filter(t => t.occurredOn.startsWith(key) && t.type === "EXPENSE")
                       .reduce((s, t) => s + Number(t.amount), 0);
      twelveMonthTrend.push({ month: key, monthLabel: label, spending: exp });
      availableMonths.push(key);
    }
    availableMonths.reverse();

    return {
      month: targetMonth,
      monthLabel,
      totalIncome,
      totalExpense,
      totalSavings,
      remainingBalance,
      highestSpendingCategory,
      highestSpendingCategoryAmount,
      budgetAmount,
      budgetRemaining,
      categoryBreakdown,
      twelveMonthTrend,
      availableMonths
    };
  }
};

// ===================================================================
// Initialization
// ===================================================================
document.addEventListener("DOMContentLoaded", () => {
  setupAuthTabs();
  setupEventListeners();
  checkAuth();
});

async function checkAuth() {
  try {
    currentUser = await api.getAuth();
    if (currentUser) {
      renderLoggedInUI();
    } else {
      renderLoggedOutUI();
    }
  } catch (err) {
    console.error("Auth check failed:", err);
    renderLoggedOutUI();
  }
}

function renderLoggedInUI() {
  document.getElementById("authSection").style.display = "none";
  document.getElementById("appMainSection").style.display = "block";
  document.getElementById("headerUserActions").style.display = "flex";

  const initials = (currentUser.fullName || currentUser.email || "U")
    .split(" ")
    .filter(Boolean)
    .slice(0, 2)
    .map(p => p[0].toUpperCase())
    .join("");
  document.getElementById("headerUserAvatar").textContent = initials;

  const now = new Date();
  document.getElementById("headerMonthPill").textContent = now.toLocaleDateString("en-IN", {
    month: "long",
    year: "numeric"
  });

  loadCurrentView();
}

function renderLoggedOutUI() {
  currentUser = null;
  document.getElementById("authSection").style.display = "flex";
  document.getElementById("appMainSection").style.display = "none";
  document.getElementById("headerUserActions").style.display = "none";
  destroyCharts();
}

function setupAuthTabs() {
  const tabSignIn = document.getElementById("tabSignIn");
  const tabSignUp = document.getElementById("tabSignUp");
  const formSignIn = document.getElementById("signInForm");
  const formSignUp = document.getElementById("signUpForm");

  tabSignIn.addEventListener("click", () => {
    tabSignIn.classList.add("active");
    tabSignUp.classList.remove("active");
    formSignIn.style.display = "block";
    formSignUp.style.display = "none";
  });

  tabSignUp.addEventListener("click", () => {
    tabSignUp.classList.add("active");
    tabSignIn.classList.remove("active");
    formSignIn.style.display = "none";
    formSignUp.style.display = "block";
  });
}

function setupEventListeners() {
  // Sign In Form
  document.getElementById("signInForm").addEventListener("submit", async (e) => {
    e.preventDefault();
    const email = document.getElementById("loginEmail").value.trim();
    const password = document.getElementById("loginPassword").value;
    const btn = document.getElementById("loginSubmitBtn");
    btn.disabled = true;
    btn.textContent = "Signing in...";

    try {
      currentUser = await api.login(email, password);
      showToast(`Welcome back, ${currentUser.fullName}!`);
      renderLoggedInUI();
    } catch (err) {
      showToast(err.message, "error");
    } finally {
      btn.disabled = false;
      btn.textContent = "Sign In";
    }
  });

  // Sign Up Form
  document.getElementById("signUpForm").addEventListener("submit", async (e) => {
    e.preventDefault();
    const fullName = document.getElementById("regName").value.trim();
    const email = document.getElementById("regEmail").value.trim();
    const password = document.getElementById("regPassword").value;
    const btn = document.getElementById("registerSubmitBtn");
    btn.disabled = true;
    btn.textContent = "Creating account...";

    try {
      currentUser = await api.register(fullName, email, password);
      showToast(`Account created! Welcome, ${currentUser.fullName}!`);
      renderLoggedInUI();
    } catch (err) {
      showToast(err.message, "error");
    } finally {
      btn.disabled = false;
      btn.textContent = "Create Account";
    }
  });

  // Demo Account Button
  document.getElementById("demoAccountBtn").addEventListener("click", async () => {
    document.getElementById("loginEmail").value = "demo@rupeeflow.com";
    document.getElementById("loginPassword").value = "password123";
    document.getElementById("signInForm").dispatchEvent(new Event("submit"));
  });

  // Sign Out
  document.getElementById("signOutBtn").addEventListener("click", async () => {
    await api.logout();
    showToast("Signed out successfully");
    renderLoggedOutUI();
  });

  // Nav Tabs
  document.querySelectorAll(".nav-tab").forEach(tab => {
    tab.addEventListener("click", () => {
      switchNavTab(tab.dataset.view);
    });
  });

  // Quick Add Button on Dashboard
  document.getElementById("openAddTxBtn").addEventListener("click", () => {
    openTransactionModal();
  });

  // Modal Form Submission
  document.getElementById("txModalForm").addEventListener("submit", handleSaveTransaction);

  // Filters on Transactions view
  const debounce = (fn, delay = 300) => {
    let timeout;
    return (...args) => {
      clearTimeout(timeout);
      timeout = setTimeout(() => fn(...args), delay);
    };
  };

  const triggerFilter = debounce(() => loadTransactionsView(), 300);
  document.getElementById("txSearchInput").addEventListener("input", triggerFilter);
  document.getElementById("txFilterType").addEventListener("change", () => loadTransactionsView());
  document.getElementById("txFilterCategory").addEventListener("change", () => loadTransactionsView());
  document.getElementById("txFilterFrom").addEventListener("change", () => loadTransactionsView());
  document.getElementById("txFilterTo").addEventListener("change", () => loadTransactionsView());

  // Budget Form
  document.getElementById("saveBudgetForm").addEventListener("submit", handleSaveBudget);

  // Goal Form
  document.getElementById("saveGoalForm").addEventListener("submit", handleSaveGoal);

  // Report Month Selector
  document.getElementById("reportMonthSelect").addEventListener("change", (e) => {
    loadReportsView(e.target.value);
  });
}

function switchNavTab(viewName) {
  currentView = viewName;
  document.querySelectorAll(".nav-tab").forEach(t => {
    t.classList.toggle("active", t.dataset.view === viewName);
  });

  document.getElementById("viewDashboard").style.display = viewName === "dashboard" ? "block" : "none";
  document.getElementById("viewTransactions").style.display = viewName === "transactions" ? "block" : "none";
  document.getElementById("viewBudget").style.display = viewName === "budget" ? "block" : "none";
  document.getElementById("viewReports").style.display = viewName === "reports" ? "block" : "none";

  loadCurrentView();
}

function loadCurrentView() {
  if (currentView === "dashboard") loadDashboardView();
  else if (currentView === "transactions") loadTransactionsView();
  else if (currentView === "budget") loadBudgetView();
  else if (currentView === "reports") loadReportsView();
}

function destroyCharts() {
  Object.values(chartInstances).forEach(chart => {
    if (chart && typeof chart.destroy === "function") chart.destroy();
  });
  chartInstances = {};
}

// ===================================================================
// VIEW 1: DASHBOARD
// ===================================================================
async function loadDashboardView() {
  try {
    const data = await api.getDashboardStats();

    // Month Heading
    document.getElementById("dashMonthHeading").textContent = `${data.monthLabel} Summary`;

    // Metrics
    document.getElementById("dashBalance").textContent = formatRupees(data.remainingBalance);
    document.getElementById("dashSavedThisMonth").textContent = `${formatRupees(data.savingsThisMonth)} saved this month`;
    document.getElementById("dashTotalIncome").textContent = formatRupees(data.totalIncome);
    document.getElementById("dashTotalExpense").textContent = formatRupees(data.totalExpense);
    document.getElementById("dashTotalSavingsAllTime").textContent = formatRupees(data.savingsTotalAllTime);

    // Goal widget
    const goalPct = Math.round(data.savingsGoalPercentage || 0);
    document.getElementById("dashGoalReachedText").textContent = data.savingsGoalTarget > 0
      ? `Savings goal ${goalPct}% reached`
      : "Set a savings goal in Budget & Goals";
    document.getElementById("dashSavingsGoalBar").style.width = `${Math.min(goalPct, 100)}%`;
    document.getElementById("dashGoalSummaryText").textContent = data.savingsGoalTarget > 0
      ? `${data.savingsGoalTitle} · Target ${formatRupees(data.savingsGoalTarget)}`
      : "No savings goal set yet";

    // Budget Alert Banner
    const alertBanner = document.getElementById("dashBudgetAlert");
    if (data.budgetAlert) {
      alertBanner.style.display = "flex";
      alertBanner.className = data.budgetUsedPercentage >= 100 ? "alert-banner danger" : "alert-banner";
      document.getElementById("dashBudgetAlertText").textContent = data.budgetAlertMessage;
    } else {
      alertBanner.style.display = "none";
    }

    // Budget Widget
    const budgetSetView = document.getElementById("dashBudgetSetView");
    const budgetNotSetView = document.getElementById("dashBudgetNotSetView");
    if (data.monthlyBudget > 0) {
      budgetSetView.style.display = "block";
      budgetNotSetView.style.display = "none";
      document.getElementById("dashBudgetRatioText").textContent =
        `${formatRupees(data.budgetUsedAmount)} / ${formatRupees(data.monthlyBudget)}`;

      const usedPct = Math.round(data.budgetUsedPercentage);
      const bar = document.getElementById("dashBudgetProgressBar");
      bar.style.width = `${Math.min(usedPct, 100)}%`;
      bar.className = `progress-bar-fill ${usedPct >= 100 ? "neg" : usedPct >= 80 ? "warn" : "pos"}`;

      document.getElementById("dashBudgetStatusText").textContent =
        `${usedPct}% used · ${formatRupees(data.budgetRemainingAmount)} remaining`;
    } else {
      budgetSetView.style.display = "none";
      budgetNotSetView.style.display = "block";
    }

    // Recent Transactions
    renderRecentTransactions(data.recentTransactions || []);

    // Render Charts
    renderIncomeExpenseBarChart(data.sixMonthTrend || []);
    renderCategoryPieChart(data.categoryBreakdown || []);

  } catch (err) {
    console.error("Dashboard error:", err);
  }
}

function renderRecentTransactions(transactions) {
  const container = document.getElementById("dashRecentTxList");
  if (!transactions || transactions.length === 0) {
    container.innerHTML = `
      <div class="empty-state">
        <p>No transactions yet.</p>
        <button class="btn btn-outline btn-sm" style="margin-top: 0.5rem;" onclick="openTransactionModal()">
          + Add First Entry
        </button>
      </div>
    `;
    return;
  }

  container.innerHTML = transactions.map(tx => {
    const isInc = tx.type === "INCOME";
    return `
      <div class="tx-item">
        <div class="tx-icon-badge ${isInc ? "income" : "expense"}">
          ${isInc ? "+" : "−"}
        </div>
        <div class="tx-details">
          <div class="tx-title">${escapeHtml(tx.description || tx.category)}</div>
          <div class="tx-meta">${formatDate(tx.occurredOn)} · ${escapeHtml(tx.category)}</div>
        </div>
        <div class="tx-amount ${isInc ? "text-pos" : "text-neg"}">
          ${isInc ? "+" : "−"}${formatRupees(tx.amount)}
        </div>
      </div>
    `;
  }).join("");
}

function renderIncomeExpenseBarChart(trendData) {
  const ctx = document.getElementById("incomeExpenseBarChart");
  if (!ctx) return;

  if (chartInstances.bar) {
    chartInstances.bar.destroy();
  }

  const labels = trendData.map(d => d.monthLabel);
  const incomeData = trendData.map(d => d.income);
  const expenseData = trendData.map(d => d.expense);

  chartInstances.bar = new Chart(ctx, {
    type: "bar",
    data: {
      labels,
      datasets: [
        {
          label: "Income",
          data: incomeData,
          backgroundColor: "#10b981",
          borderRadius: 6,
          barPercentage: 0.6,
          categoryPercentage: 0.7
        },
        {
          label: "Expense",
          data: expenseData,
          backgroundColor: "#f43f5e",
          borderRadius: 6,
          barPercentage: 0.6,
          categoryPercentage: 0.7
        }
      ]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: { display: false },
        tooltip: {
          backgroundColor: "#182234",
          borderColor: "rgba(255,255,255,0.12)",
          borderWidth: 1,
          padding: 10,
          callbacks: {
            label: (ctx) => `${ctx.dataset.label}: ${formatRupees(ctx.raw)}`
          }
        }
      },
      scales: {
        x: {
          grid: { display: false },
          ticks: { color: "#94a3b8", font: { family: "Inter", size: 11 } }
        },
        y: {
          grid: { color: "rgba(255, 255, 255, 0.05)" },
          ticks: {
            color: "#94a3b8",
            font: { family: "Inter", size: 11 },
            callback: (v) => formatRupees(v)
          }
        }
      }
    }
  });
}

function renderCategoryPieChart(categories) {
  const ctx = document.getElementById("categoryPieChart");
  const legend = document.getElementById("dashCategoryLegend");
  const empty = document.getElementById("dashCategoryEmpty");
  const content = document.getElementById("dashCategoryContent");

  if (!ctx) return;

  if (chartInstances.pie) {
    chartInstances.pie.destroy();
  }

  if (!categories || categories.length === 0) {
    empty.style.display = "block";
    content.style.display = "none";
    return;
  }

  empty.style.display = "none";
  content.style.display = "block";

  const labels = categories.map(c => c.category);
  const data = categories.map(c => c.amount);
  const backgroundColors = categories.map((_, i) => PIE_COLORS[i % PIE_COLORS.length]);

  chartInstances.pie = new Chart(ctx, {
    type: "doughnut",
    data: {
      labels,
      datasets: [{
        data,
        backgroundColor: backgroundColors,
        borderWidth: 0,
        hoverOffset: 6
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      cutout: "68%",
      plugins: {
        legend: { display: false },
        tooltip: {
          backgroundColor: "#182234",
          borderColor: "rgba(255,255,255,0.12)",
          borderWidth: 1,
          padding: 10,
          callbacks: {
            label: (ctx) => `${ctx.label}: ${formatRupees(ctx.raw)} (${categories[ctx.dataIndex].percentage.toFixed(1)}%)`
          }
        }
      }
    }
  });

  legend.innerHTML = categories.slice(0, 5).map((c, i) => `
    <div class="legend-row">
      <div class="legend-tag">
        <span class="legend-color-dot" style="background: ${PIE_COLORS[i % PIE_COLORS.length]};"></span>
        <span style="color: var(--text-main);">${escapeHtml(c.category)}</span>
      </div>
      <span style="color: var(--text-muted);">${formatRupees(c.amount)} (${Math.round(c.percentage)}%)</span>
    </div>
  `).join("");
}

// ===================================================================
// VIEW 2: TRANSACTIONS
// ===================================================================
async function loadTransactionsView() {
  const type = document.getElementById("txFilterType").value;
  const category = document.getElementById("txFilterCategory").value;
  const from = document.getElementById("txFilterFrom").value;
  const to = document.getElementById("txFilterTo").value;
  const search = document.getElementById("txSearchInput").value.trim();

  const params = new URLSearchParams();
  if (type) params.append("type", type);
  if (category) params.append("category", category);
  if (from) params.append("from", from);
  if (to) params.append("to", to);
  if (search) params.append("search", search);

  try {
    const transactions = await api.getTransactions(params);

    const income = transactions.filter(t => t.type === "INCOME").reduce((s, t) => s + Number(t.amount), 0);
    const expense = transactions.filter(t => t.type === "EXPENSE").reduce((s, t) => s + Number(t.amount), 0);

    document.getElementById("txFilterSummaryCount").textContent = `${transactions.length} entries found`;
    document.getElementById("txFilterSummaryIncome").textContent = `Income: ${formatRupees(income)}`;
    document.getElementById("txFilterSummaryExpense").textContent = `Expense: ${formatRupees(expense)}`;

    const container = document.getElementById("allTxList");
    if (transactions.length === 0) {
      container.innerHTML = `
        <div class="empty-state">
          <p>No matching transactions found.</p>
        </div>
      `;
      return;
    }

    container.innerHTML = transactions.map(tx => {
      const isInc = tx.type === "INCOME";
      return `
        <div class="tx-item">
          <div class="tx-icon-badge ${isInc ? "income" : "expense"}">
            ${isInc ? "+" : "−"}
          </div>
          <div class="tx-details">
            <div class="tx-title">${escapeHtml(tx.description || tx.category)}</div>
            <div class="tx-meta">${formatDate(tx.occurredOn)} · ${escapeHtml(tx.category)}</div>
          </div>
          <div class="tx-amount ${isInc ? "text-pos" : "text-neg"}">
            ${isInc ? "+" : "−"}${formatRupees(tx.amount)}
          </div>
          <div class="tx-actions">
            <button class="action-btn" title="Edit" onclick="editTransaction(${tx.id}, '${tx.type}', '${escapeAttr(tx.category)}', ${tx.amount}, '${tx.occurredOn}', '${escapeAttr(tx.description || '')}')">
              ✏️
            </button>
            <button class="action-btn delete" title="Delete" onclick="deleteTransaction(${tx.id})">
              🗑️
            </button>
          </div>
        </div>
      `;
    }).join("");

  } catch (err) {
    console.error("Transactions fetch failed:", err);
  }
}

async function deleteTransaction(id) {
  if (!confirm("Are you sure you want to delete this transaction?")) return;

  try {
    await api.deleteTransaction(id);
    showToast("Transaction deleted");
    loadTransactionsView();
  } catch (err) {
    showToast(err.message, "error");
  }
}

// ===================================================================
// VIEW 3: BUDGET & GOALS
// ===================================================================
async function loadBudgetView() {
  try {
    const [budgetData, goalData, statsData] = await Promise.all([
      api.getBudget(),
      api.getSavingsGoal(),
      api.getDashboardStats()
    ]);

    document.getElementById("budgetCardMonthSubtitle").textContent = `For ${statsData.monthLabel}`;
    if (budgetData.amount > 0) {
      document.getElementById("budgetInputAmount").value = budgetData.amount;
      document.getElementById("budgetProgressContainer").style.display = "block";
      document.getElementById("budgetRatioDisplay").textContent =
        `${formatRupees(statsData.budgetUsedAmount)} / ${formatRupees(budgetData.amount)}`;

      const pct = Math.round(statsData.budgetUsedPercentage);
      const bar = document.getElementById("budgetDetailProgressBar");
      bar.style.width = `${Math.min(pct, 100)}%`;
      bar.className = `progress-bar-fill ${pct >= 100 ? "neg" : pct >= 80 ? "warn" : "pos"}`;

      document.getElementById("budgetDetailStatus").textContent =
        `${pct}% used · ${formatRupees(statsData.budgetRemainingAmount)} remaining`;

      const alertBox = document.getElementById("budgetAlertWarningBox");
      if (statsData.budgetAlert) {
        alertBox.style.display = "flex";
        alertBox.className = pct >= 100 ? "alert-banner danger" : "alert-banner";
        document.getElementById("budgetAlertWarningText").textContent = statsData.budgetAlertMessage;
      } else {
        alertBox.style.display = "none";
      }
    } else {
      document.getElementById("budgetInputAmount").value = "";
      document.getElementById("budgetProgressContainer").style.display = "none";
    }

    if (goalData.targetAmount > 0) {
      document.getElementById("goalInputTitle").value = goalData.title;
      document.getElementById("goalInputAmount").value = goalData.targetAmount;
      document.getElementById("goalProgressContainer").style.display = "block";

      const saved = statsData.savingsTotalAllTime;
      const target = goalData.targetAmount;
      const pct = Math.min(100, Math.round((saved / target) * 100));

      document.getElementById("goalRatioDisplay").textContent = `${formatRupees(saved)} / ${formatRupees(target)}`;
      document.getElementById("goalDetailProgressBar").style.width = `${pct}%`;
      document.getElementById("goalDetailStatus").textContent = `${pct}% reached · ${formatRupees(Math.max(target - saved, 0))} to target`;
    } else {
      document.getElementById("goalInputTitle").value = "";
      document.getElementById("goalInputAmount").value = "";
      document.getElementById("goalProgressContainer").style.display = "none";
    }

  } catch (err) {
    console.error("Budget view error:", err);
  }
}

async function handleSaveBudget(e) {
  e.preventDefault();
  const amount = Number(document.getElementById("budgetInputAmount").value);
  if (!amount || amount <= 0) {
    showToast("Please enter a valid budget amount", "error");
    return;
  }

  const now = new Date();
  const month = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}`;

  try {
    await api.saveBudget(month, amount);
    showToast("Monthly budget saved!");
    loadBudgetView();
  } catch (err) {
    showToast(err.message, "error");
  }
}

async function handleSaveGoal(e) {
  e.preventDefault();
  const title = document.getElementById("goalInputTitle").value.trim();
  const targetAmount = Number(document.getElementById("goalInputAmount").value);
  if (!targetAmount || targetAmount <= 0) {
    showToast("Please enter a valid target amount", "error");
    return;
  }

  try {
    await api.saveSavingsGoal(title, targetAmount);
    showToast("Savings goal saved!");
    loadBudgetView();
  } catch (err) {
    showToast(err.message, "error");
  }
}

// ===================================================================
// VIEW 4: MONTHLY REPORT
// ===================================================================
async function loadReportsView(selectedMonth = "") {
  try {
    const data = await api.getReports(selectedMonth);

    const select = document.getElementById("reportMonthSelect");
    if (data.availableMonths && select.options.length === 0) {
      select.innerHTML = data.availableMonths.map(m => {
        const [y, mm] = m.split("-");
        const date = new Date(Number(y), Number(mm) - 1, 1);
        const label = date.toLocaleDateString("en-IN", { month: "short", year: "numeric" });
        return `<option value="${m}">${label}</option>`;
      }).join("");
    }
    if (data.month) select.value = data.month;

    document.getElementById("repTotalIncome").textContent = formatRupees(data.totalIncome);
    document.getElementById("repTotalExpense").textContent = formatRupees(data.totalExpense);
    document.getElementById("repTotalSavings").textContent = formatRupees(data.totalSavings);
    document.getElementById("repBalance").textContent = formatRupees(data.remainingBalance);

    document.getElementById("repHighestCategory").textContent = data.highestSpendingCategory;
    document.getElementById("repHighestCategoryAmt").textContent = data.highestSpendingCategoryAmount > 0
      ? formatRupees(data.highestSpendingCategoryAmount)
      : "No expenses logged";

    if (data.budgetAmount > 0) {
      document.getElementById("repBudgetAmount").textContent = formatRupees(data.budgetAmount);
      document.getElementById("repBudgetRemaining").textContent = `${formatRupees(data.budgetRemaining)} remaining`;
    } else {
      document.getElementById("repBudgetAmount").textContent = "Not set";
      document.getElementById("repBudgetRemaining").textContent = "Set one in Budget & Goals";
    }

    renderSpendingTrendChart(data.twelveMonthTrend || []);

  } catch (err) {
    console.error("Report error:", err);
  }
}

function renderSpendingTrendChart(trendData) {
  const ctx = document.getElementById("spendingTrendChart");
  if (!ctx) return;

  if (chartInstances.area) {
    chartInstances.area.destroy();
  }

  const labels = trendData.map(d => d.monthLabel);
  const data = trendData.map(d => d.spending);

  chartInstances.area = new Chart(ctx, {
    type: "line",
    data: {
      labels,
      datasets: [{
        label: "Spending",
        data,
        borderColor: "#a855f7",
        backgroundColor: "rgba(168, 85, 247, 0.15)",
        fill: true,
        tension: 0.35,
        borderWidth: 2.5,
        pointBackgroundColor: "#a855f7",
        pointRadius: 3.5,
        pointHoverRadius: 6
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: { display: false },
        tooltip: {
          backgroundColor: "#182234",
          borderColor: "rgba(255,255,255,0.12)",
          borderWidth: 1,
          padding: 10,
          callbacks: {
            label: (ctx) => `Spent: ${formatRupees(ctx.raw)}`
          }
        }
      },
      scales: {
        x: {
          grid: { display: false },
          ticks: { color: "#94a3b8", font: { family: "Inter", size: 10 } }
        },
        y: {
          grid: { color: "rgba(255, 255, 255, 0.05)" },
          ticks: {
            color: "#94a3b8",
            font: { family: "Inter", size: 10 },
            callback: (v) => formatRupees(v)
          }
        }
      }
    }
  });
}

// ===================================================================
// ADD / EDIT TRANSACTION MODAL
// ===================================================================
function openTransactionModal(isEdit = false) {
  document.getElementById("txModalTitle").textContent = isEdit ? "Edit Transaction" : "Add Transaction";
  document.getElementById("saveTxModalBtn").textContent = isEdit ? "Update Transaction" : "Save Transaction";

  if (!isEdit) {
    document.getElementById("txModalId").value = "";
    document.getElementById("txModalAmount").value = "";
    document.getElementById("txModalDesc").value = "";
    document.getElementById("txModalDate").value = new Date().toISOString().slice(0, 10);
    selectModalType("EXPENSE");
  }

  document.getElementById("txModalBackdrop").classList.add("open");
}

function closeTransactionModal() {
  document.getElementById("txModalBackdrop").classList.remove("open");
}

function selectModalType(type) {
  selectedModalType = type;
  const btnExpense = document.getElementById("typeExpenseBtn");
  const btnIncome = document.getElementById("typeIncomeBtn");

  if (type === "EXPENSE") {
    btnExpense.className = "type-toggle-btn active expense";
    btnIncome.className = "type-toggle-btn income";
  } else {
    btnExpense.className = "type-toggle-btn expense";
    btnIncome.className = "type-toggle-btn active income";
  }

  const categories = type === "EXPENSE" ? EXPENSE_CATEGORIES : INCOME_CATEGORIES;
  const select = document.getElementById("txModalCategory");
  select.innerHTML = categories.map(c => `<option value="${c}">${c}</option>`).join("");
}

function editTransaction(id, type, category, amount, date, description) {
  document.getElementById("txModalId").value = id;
  selectModalType(type);
  document.getElementById("txModalCategory").value = category;
  document.getElementById("txModalAmount").value = amount;
  document.getElementById("txModalDate").value = date;
  document.getElementById("txModalDesc").value = description;

  openTransactionModal(true);
}

async function handleSaveTransaction(e) {
  e.preventDefault();
  const id = document.getElementById("txModalId").value;
  const amount = Number(document.getElementById("txModalAmount").value);
  const category = document.getElementById("txModalCategory").value;
  const occurredOn = document.getElementById("txModalDate").value;
  const description = document.getElementById("txModalDesc").value.trim();

  if (!amount || amount <= 0) {
    showToast("Please enter an amount greater than 0", "error");
    return;
  }

  const payload = {
    type: selectedModalType,
    category,
    amount,
    occurredOn,
    description
  };

  const btn = document.getElementById("saveTxModalBtn");
  btn.disabled = true;

  try {
    await api.saveTransaction(id, payload);
    showToast(id ? "Transaction updated!" : "Transaction added!");
    closeTransactionModal();
    loadCurrentView();
  } catch (err) {
    showToast(err.message, "error");
  } finally {
    btn.disabled = false;
  }
}

// Helpers: Escape HTML & Attributes
function escapeHtml(str) {
  if (!str) return "";
  return String(str).replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");
}

function escapeAttr(str) {
  if (!str) return "";
  return String(str).replace(/'/g, "\\'").replace(/"/g, "&quot;");
}
