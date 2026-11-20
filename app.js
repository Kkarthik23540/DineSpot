/**
 * DineSpot Web Application - Dual Portal Engine (Diner & Admin)
 */

// Firebase Configuration from project google-services.json
const firebaseConfig = {
  apiKey: "AIzaSyCoj6JbhKbhBlRZeiICqrBKgGHNDWht6Dw",
  authDomain: "dinespot-91d4b.firebaseapp.com",
  databaseURL: "https://dinespot-91d4b-default-rtdb.firebaseio.com",
  projectId: "dinespot-91d4b",
  storageBucket: "dinespot-91d4b.firebasestorage.app",
  messagingSenderId: "1072182836759",
  appId: "1:1072182836759:web:dinespot"
};

// Initialize Firebase SDKs
firebase.initializeApp(firebaseConfig);
const auth = firebase.auth();
const db = firebase.firestore();
const rtdb = firebase.database();

// Global App State
let currentUser = null;
let currentRole = 'user'; // 'user' or 'admin'
let cart = [];
let activeRestaurant = null;
let restaurants = [];
let foodItems = [];
let bookings = [];
let chats = [];

// Sample Mock Initial Data for Firestore when fresh
const sampleRestaurants = [
  {
    name: "La Bella Italia",
    address: "123 Gourmet Street, Downtown",
    category: "Italian",
    imageUrl: "https://images.unsplash.com/photo-1555396273-367ea4eb4db5?auto=format&fit=crop&w=600&q=80",
    rating: 4.8,
    noOfTables: 12,
    phoneNumber: "+91 9876543210",
    openingTime: "11:00 AM",
    closingTime: "11:00 PM",
    status: "Open",
    tableTypes: { "2": 4, "4": 4, "6": 2, "10": 2 }
  },
  {
    name: "Royal Spice Indian Bistro",
    address: "45 Heritage Avenue, Midtown",
    category: "Indian",
    imageUrl: "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?auto=format&fit=crop&w=600&q=80",
    rating: 4.9,
    noOfTables: 15,
    phoneNumber: "+91 9876543211",
    openingTime: "12:00 PM",
    closingTime: "11:30 PM",
    status: "Open",
    tableTypes: { "2": 5, "4": 5, "6": 3, "10": 2 }
  },
  {
    name: "Urban Burger & Brew",
    address: "88 Culinary Hub, Westside",
    category: "Fast Food",
    imageUrl: "https://images.unsplash.com/photo-1550547660-d9450f859349?auto=format&fit=crop&w=600&q=80",
    rating: 4.6,
    noOfTables: 10,
    phoneNumber: "+91 9876543212",
    openingTime: "10:00 AM",
    closingTime: "10:00 PM",
    status: "Open",
    tableTypes: { "2": 3, "4": 4, "6": 2, "10": 1 }
  }
];

const sampleFoodItems = [
  { name: "Margherita Pizza", price: 399, category: "Main Course", veg: true, imageUrl: "https://images.unsplash.com/photo-1604382354936-07c5d9983bd3?auto=format&fit=crop&w=400&q=80" },
  { name: "Creamy Alfredo Pasta", price: 349, category: "Main Course", veg: true, imageUrl: "https://images.unsplash.com/photo-1621996346565-e3d5d6281270?auto=format&fit=crop&w=400&q=80" },
  { name: "Paneer Tikka Masala", price: 380, category: "Main Course", veg: true, imageUrl: "https://images.unsplash.com/photo-1565557623262-b51c2513a641?auto=format&fit=crop&w=400&q=80" },
  { name: "Butter Chicken", price: 420, category: "Main Course", veg: false, imageUrl: "https://images.unsplash.com/photo-1588166524941-3bf61a9c41db?auto=format&fit=crop&w=400&q=80" },
  { name: "Classic Cheeseburger", price: 249, category: "Starters", veg: false, imageUrl: "https://images.unsplash.com/photo-1568901346375-23c9450c58cd?auto=format&fit=crop&w=400&q=80" },
  { name: "Tiramisu Dessert", price: 199, category: "Desserts", veg: true, imageUrl: "https://images.unsplash.com/photo-1571877227200-a0d98ea607e9?auto=format&fit=crop&w=400&q=80" }
];

// Initialize Web App
document.addEventListener('DOMContentLoaded', () => {
  setupEventListeners();
  listenToAuth();
  fetchRestaurants();
  fetchBookings();
});

// Event Listeners setup
function setupEventListeners() {
  document.getElementById('roleToggleBtn')?.addEventListener('click', toggleRole);
  document.getElementById('cartNavBtn')?.addEventListener('click', toggleCartDrawer);
  document.getElementById('closeCartBtn')?.addEventListener('click', toggleCartDrawer);
  document.getElementById('chatNavBtn')?.addEventListener('click', openChatModal);
  document.getElementById('themeToggleBtn')?.addEventListener('click', toggleTheme);
  document.getElementById('searchInput')?.addEventListener('input', handleSearch);
  document.getElementById('checkoutBtn')?.addEventListener('click', initiateCheckout);
}

// Auth State Monitor
function listenToAuth() {
  auth.onAuthStateChanged(user => {
    currentUser = user;
    const authBtn = document.getElementById('authNavBtn');
    if (user) {
      authBtn.innerHTML = `<i class="fas fa-sign-out-alt"></i> Logout (${user.email.split('@')[0]})`;
      authBtn.onclick = () => auth.signOut();
    } else {
      authBtn.innerHTML = `<i class="fas fa-sign-in-alt"></i> Login / Sign Up`;
      authBtn.onclick = () => openAuthModal();
    }
  });
}

// Toggle Role (User vs Admin)
function toggleRole() {
  currentRole = currentRole === 'user' ? 'admin' : 'user';
  const roleBtn = document.getElementById('roleToggleBtn');
  const userPortal = document.getElementById('userPortal');
  const adminPortal = document.getElementById('adminPortal');

  if (currentRole === 'admin') {
    roleBtn.innerHTML = `<i class="fas fa-user-shield"></i> Switch to Diner Portal`;
    roleBtn.style.borderColor = '#FF5722';
    roleBtn.style.color = '#FF5722';
    userPortal.style.display = 'none';
    adminPortal.style.display = 'block';
    renderAdminDashboard();
  } else {
    roleBtn.innerHTML = `<i class="fas fa-store"></i> Switch to Admin Portal`;
    roleBtn.style.borderColor = '#26A69A';
    roleBtn.style.color = '#26A69A';
    userPortal.style.display = 'block';
    adminPortal.style.display = 'none';
    renderUserPortal();
  }
}

// Fetch Restaurants from Firestore
async function fetchRestaurants() {
  try {
    const snapshot = await db.collection('restaurants').get();
    if (snapshot.empty) {
      // Seed Firestore with initial sample data if empty
      for (const res of sampleRestaurants) {
        const docRef = await db.collection('restaurants').add(res);
        restaurants.push({ id: docRef.id, ...res });
      }
    } else {
      restaurants = snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() }));
    }
    renderRestaurants(restaurants);
    fetchFoodItems();
  } catch (e) {
    console.error("Firestore error:", e);
    restaurants = sampleRestaurants.map((r, i) => ({ id: `res_${i}`, ...r }));
    renderRestaurants(restaurants);
  }
}

// Fetch Food Items
async function fetchFoodItems() {
  try {
    const snapshot = await db.collection('food_items').get();
    if (snapshot.empty && restaurants.length > 0) {
      for (const food of sampleFoodItems) {
        food.restaurantId = restaurants[0].id;
        const docRef = await db.collection('food_items').add(food);
        foodItems.push({ id: docRef.id, ...food });
      }
    } else {
      foodItems = snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() }));
    }
  } catch (e) {
    foodItems = sampleFoodItems.map((f, i) => ({ id: `food_${i}`, ...f }));
  }
}

// Fetch Bookings
async function fetchBookings() {
  try {
    const snapshot = await db.collection('bookings').get();
    bookings = snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() }));
  } catch (e) {
    bookings = [];
  }
}

// Render User Restaurants Grid
function renderRestaurants(list) {
  const container = document.getElementById('restaurantGrid');
  if (!container) return;

  if (list.length === 0) {
    container.innerHTML = `<p style="grid-column: 1/-1; text-align: center; color: var(--text-muted);">No restaurants found.</p>`;
    return;
  }

  container.innerHTML = list.map(r => `
    <div class="card" onclick="openRestaurantDetails('${r.id}')">
      <div class="card-img-wrapper">
        <img src="${r.imageUrl}" class="card-img" alt="${r.name}">
        <span class="badge-tag">${r.category || 'Multi-Cuisine'}</span>
        <button class="fav-btn" onclick="event.stopPropagation(); toggleFavorite('${r.id}')">
          <i class="far fa-heart"></i>
        </button>
      </div>
      <div class="card-content">
        <div class="card-title">${r.name}</div>
        <div class="card-meta">
          <span class="rating-badge"><i class="fas fa-star"></i> ${r.rating || 4.5}</span>
          <span><i class="far fa-clock"></i> ${r.openingTime || '11 AM'} - ${r.closingTime || '11 PM'}</span>
        </div>
        <p style="font-size: 0.85rem; color: var(--text-muted); margin-bottom: 1rem;"><i class="fas fa-map-marker-alt"></i> ${r.address}</p>
        <button class="btn btn-primary btn-sm btn-block">Reserve Table & View Menu</button>
      </div>
    </div>
  `).join('');
}

// Category Filter Click
function filterCategory(cat, element) {
  document.querySelectorAll('.chip').forEach(c => c.classList.remove('active'));
  element.classList.add('active');

  if (cat === 'All') {
    renderRestaurants(restaurants);
  } else {
    const filtered = restaurants.filter(r => r.category === cat);
    renderRestaurants(filtered);
  }
}

// Handle Search
function handleSearch(e) {
  const query = e.target.value.toLowerCase();
  const filtered = restaurants.filter(r =>
    r.name.toLowerCase().includes(query) ||
    (r.category && r.category.toLowerCase().includes(query)) ||
    r.address.toLowerCase().includes(query)
  );
  renderRestaurants(filtered);
}

// Open Restaurant Details Modal
function openRestaurantDetails(resId) {
  activeRestaurant = restaurants.find(r => r.id === resId);
  if (!activeRestaurant) return;

  const modal = document.getElementById('restaurantModal');
  const items = foodItems.filter(f => f.restaurantId === resId || !f.restaurantId);

  document.getElementById('modalResName').innerText = activeRestaurant.name;
  document.getElementById('modalResAddress').innerText = activeRestaurant.address;

  const menuContainer = document.getElementById('modalMenuGrid');
  menuContainer.innerHTML = items.map(item => `
    <div class="cart-item">
      <div style="display: flex; gap: 1rem; align-items: center;">
        <img src="${item.imageUrl}" style="width: 60px; height: 60px; border-radius: 8px; object-fit: cover;">
        <div>
          <div style="font-weight: 600;">${item.name} ${item.veg ? '🟢' : '🔴'}</div>
          <div style="color: var(--primary); font-weight: 700;">₹${item.price}</div>
        </div>
      </div>
      <button class="btn btn-primary btn-sm" onclick="addToCart('${item.id}')">+ Add</button>
    </div>
  `).join('');

  modal.classList.add('active');
}

function closeModal(modalId) {
  document.getElementById(modalId).classList.remove('active');
}

// Cart System
function addToCart(foodId) {
  const item = foodItems.find(f => f.id === foodId);
  if (!item) return;

  const existing = cart.find(c => c.id === foodId);
  if (existing) {
    existing.quantity += 1;
  } else {
    cart.push({ ...item, quantity: 1 });
  }

  updateCartUI();
  toggleCartDrawer(true);
}

function updateCartUI() {
  const cartList = document.getElementById('cartItemsList');
  const countBadge = document.getElementById('cartCount');

  const totalItems = cart.reduce((sum, item) => sum + item.quantity, 0);
  countBadge.innerText = totalItems;

  if (cart.length === 0) {
    cartList.innerHTML = `<p style="text-align: center; color: var(--text-muted); margin-top: 2rem;">Your cart is empty.</p>`;
    document.getElementById('cartSubtotal').innerText = "₹0";
    document.getElementById('cartTotal').innerText = "₹0";
    return;
  }

  cartList.innerHTML = cart.map(item => `
    <div class="cart-item">
      <div>
        <div style="font-weight: 600;">${item.name}</div>
        <div style="color: var(--primary); font-weight: 700;">₹${item.price * item.quantity}</div>
      </div>
      <div style="display: flex; align-items: center; gap: 0.5rem;">
        <button class="btn btn-secondary btn-sm" onclick="changeQuantity('${item.id}', -1)">-</button>
        <span>${item.quantity}</span>
        <button class="btn btn-secondary btn-sm" onclick="changeQuantity('${item.id}', 1)">+</button>
      </div>
    </div>
  `).join('');

  const subtotal = cart.reduce((sum, i) => sum + (i.price * i.quantity), 0);
  const total = subtotal + 40; // ₹40 delivery/taxes

  document.getElementById('cartSubtotal').innerText = `₹${subtotal}`;
  document.getElementById('cartTotal').innerText = `₹${total}`;
}

function changeQuantity(id, delta) {
  const item = cart.find(c => c.id === id);
  if (!item) return;

  item.quantity += delta;
  if (item.quantity <= 0) {
    cart = cart.filter(c => c.id !== id);
  }
  updateCartUI();
}

function toggleCartDrawer(forceOpen) {
  const drawer = document.getElementById('cartDrawer');
  if (forceOpen === true) {
    drawer.classList.add('active');
  } else {
    drawer.classList.toggle('active');
  }
}

// Razorpay Payment Integration
function initiateCheckout() {
  if (cart.length === 0) {
    alert("Cart is empty!");
    return;
  }

  const subtotal = cart.reduce((sum, i) => sum + (i.price * i.quantity), 0);
  const totalAmount = (subtotal + 40) * 100; // Razorpay expects amount in paise

  const options = {
    key: "rzp_test_THNrx21lyjVEE6", // Test Razorpay Key ID
    amount: totalAmount,
    currency: "INR",
    name: "DineSpot Order",
    description: "Food Order & Table Reservation Payment",
    image: "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?auto=format&fit=crop&w=100&q=80",
    handler: function (response) {
      alert("Payment Successful! Payment ID: " + response.razorpay_payment_id);
      saveOrderToFirestore(response.razorpay_payment_id);
    },
    prefill: {
      name: currentUser ? currentUser.email.split('@')[0] : "Guest Diner",
      email: currentUser ? currentUser.email : "guest@dinespot.com",
      contact: "9876543210"
    },
    theme: {
      color: "#FF5722"
    }
  };

  const rzp = new Razorpay(options);
  rzp.open();
}

// Save Order/Booking to Firestore
async function saveOrderToFirestore(paymentId) {
  const newBooking = {
    restaurantName: activeRestaurant ? activeRestaurant.name : "DineSpot Partner",
    restaurantId: activeRestaurant ? activeRestaurant.id : "res_0",
    userId: currentUser ? currentUser.uid : "guest_user",
    userName: currentUser ? currentUser.email.split('@')[0] : "Guest Diner",
    phoneNumber: "9876543210",
    dateTime: new Date().toLocaleDateString(),
    timeSlot: "07:30 PM",
    tableType: "4 Seater",
    status: "Accepted",
    orderStatus: "Preparing",
    paymentId: paymentId,
    orderedItems: cart.map(i => ({ name: i.name, price: i.price, quantity: i.quantity }))
  };

  try {
    const docRef = await db.collection('bookings').add(newBooking);
    bookings.push({ id: docRef.id, ...newBooking });
    cart = [];
    updateCartUI();
    toggleCartDrawer(false);
    alert("Booking & Order Confirmed!");
  } catch (e) {
    console.error("Error saving booking:", e);
  }
}

// Admin Portal Engine
function renderAdminDashboard() {
  document.getElementById('adminTotalRevenue').innerText = `₹${(bookings.length * 850).toLocaleString()}`;
  document.getElementById('adminActiveBookings').innerText = bookings.length;
  document.getElementById('adminTotalRestaurants').innerText = restaurants.length;

  renderFloorPlanGrid();
  renderAdminBookingsList();
  renderAnalyticsChart();
}

// Render Table Floor Plan
function renderFloorPlanGrid() {
  const floorGrid = document.getElementById('adminFloorGrid');
  if (!floorGrid) return;

  const tables = [
    { num: 1, type: '2 Seater', status: 'available' },
    { num: 2, type: '4 Seater', status: 'reserved' },
    { num: 3, type: '4 Seater', status: 'occupied' },
    { num: 4, type: '6 Seater', status: 'available' },
    { num: 5, type: '2 Seater', status: 'reserved' },
    { num: 6, type: '10 Seater', status: 'available' },
    { num: 7, type: '4 Seater', status: 'occupied' },
    { num: 8, type: '4 Seater', status: 'available' }
  ];

  floorGrid.innerHTML = tables.map(t => `
    <div class="table-node ${t.status}" onclick="toggleTableStatus(this)">
      <div style="font-weight: 700; font-size: 1.1rem;">Table ${t.num}</div>
      <div style="font-size: 0.8rem; margin: 0.3rem 0;">${t.type}</div>
      <span class="badge-tag" style="position: static; text-transform: capitalize;">${t.status}</span>
    </div>
  `).join('');
}

function toggleTableStatus(el) {
  if (el.classList.contains('available')) {
    el.className = 'table-node reserved';
    el.querySelector('.badge-tag').innerText = 'reserved';
  } else if (el.classList.contains('reserved')) {
    el.className = 'table-node occupied';
    el.querySelector('.badge-tag').innerText = 'occupied';
  } else {
    el.className = 'table-node available';
    el.querySelector('.badge-tag').innerText = 'available';
  }
}

// Render Bookings in Admin
function renderAdminBookingsList() {
  const container = document.getElementById('adminBookingsTable');
  if (!container) return;

  if (bookings.length === 0) {
    container.innerHTML = `<tr><td colspan="6" style="text-align: center; color: var(--text-muted);">No active reservations.</td></tr>`;
    return;
  }

  container.innerHTML = bookings.map(b => `
    <tr>
      <td>${b.userName || 'Diner'}</td>
      <td>${b.restaurantName || 'DineSpot'}</td>
      <td>${b.dateTime || 'Today'} (${b.timeSlot || '7 PM'})</td>
      <td>${b.tableType || '4-Seater'}</td>
      <td><span class="badge-tag" style="position: static; background: var(--success);">${b.status || 'Accepted'}</span></td>
      <td>
        <button class="btn btn-secondary btn-sm" onclick="updateBookingStatus('${b.id}', 'Completed')">Complete</button>
      </td>
    </tr>
  `).join('');
}

// Render Analytics Chart
function renderAnalyticsChart() {
  const ctx = document.getElementById('analyticsChart')?.getContext('2d');
  if (!ctx) return;

  if (window.myDineChart) {
    window.myDineChart.destroy();
  }

  window.myDineChart = new Chart(ctx, {
    type: 'line',
    data: {
      labels: ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'],
      datasets: [{
        label: 'Weekly Revenue (₹)',
        data: [12000, 19000, 15000, 24000, 32000, 48000, 52000],
        borderColor: '#FF5722',
        backgroundColor: 'rgba(255, 87, 34, 0.1)',
        fill: true,
        tension: 0.4
      }]
    },
    options: {
      responsive: true,
      plugins: {
        legend: { display: true }
      }
    }
  });
}

// Live Chat System
function openChatModal() {
  document.getElementById('chatModal').classList.add('active');
  listenToLiveChat();
}

function listenToLiveChat() {
  const userId = currentUser ? currentUser.uid : "guest_user";
  const chatRef = rtdb.ref(`chats/${userId}`);

  chatRef.on('value', snapshot => {
    const data = snapshot.val();
    const chatContainer = document.getElementById('chatMessages');
    if (!data) {
      chatContainer.innerHTML = `<p style="text-align: center; color: var(--text-muted);">Start conversation with support...</p>`;
      return;
    }

    const messages = Object.values(data);
    chatContainer.innerHTML = messages.map(m => `
      <div class="chat-bubble ${m.senderId === userId ? 'sent' : 'received'}">
        ${m.message}
      </div>
    `).join('');
    chatContainer.scrollTop = chatContainer.scrollHeight;
  });
}

function sendChatMessage() {
  const input = document.getElementById('chatInput');
  const msg = input.value.trim();
  if (!msg) return;

  const userId = currentUser ? currentUser.uid : "guest_user";
  const chatRef = rtdb.ref(`chats/${userId}`);

  chatRef.push({
    message: msg,
    senderId: userId,
    timestamp: Date.now()
  });

  input.value = '';
}

// Theme Toggle
function toggleTheme() {
  const currentTheme = document.body.getAttribute('data-theme');
  const newTheme = currentTheme === 'dark' ? 'light' : 'dark';
  document.body.setAttribute('data-theme', newTheme);
}

// Auth Modal
function openAuthModal() {
  const email = prompt("Enter your email to Login/Sign Up:");
  if (email) {
    auth.signInWithEmailAndPassword(email, "password123").catch(() => {
      auth.createUserWithEmailAndPassword(email, "password123").then(() => alert("Signed up successfully!"));
    });
  }
}
