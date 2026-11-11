package com.joymilk.app.ui.navigation

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.joymilk.app.data.repository.AuthRepository
import com.joymilk.app.ui.auth.LoginScreen
import com.joymilk.app.ui.dashboard.DashboardScreen
import com.joymilk.app.ui.order.OrderDetailScreen
import com.joymilk.app.ui.order.OrderFormScreen
import com.joymilk.app.ui.order.OrderListScreen
import com.joymilk.app.ui.product.PriceHistoryScreen
import com.joymilk.app.ui.product.ProductFormScreen
import com.joymilk.app.ui.product.ProductListScreen
import com.joymilk.app.ui.report.ReportScreen
import com.joymilk.app.ui.user.UserFormScreen
import com.joymilk.app.ui.user.UserListScreen
import com.joymilk.app.ui.more.MoreScreen
import com.joymilk.app.util.Constants

object Routes {
    const val LOGIN = "login"
    const val DASHBOARD = "dashboard"
    const val ORDERS = "orders"
    const val ORDER_NEW = "orders/new"
    const val ORDER_DETAIL = "orders/{orderId}"
    const val ORDER_EDIT = "orders/{orderId}/edit"
    const val PRODUCTS = "products"
    const val PRODUCT_NEW = "products/new"
    const val PRODUCT_EDIT = "products/{productId}/edit"
    const val PRICE_HISTORY = "products/{productId}/price-history"
    const val REPORTS = "reports"
    const val USERS = "users"
    const val USER_NEW = "users/new"
    const val MORE = "more"

    fun orderDetail(orderId: String) = "orders/$orderId"
    fun orderEdit(orderId: String) = "orders/$orderId/edit"
    fun productEdit(productId: String) = "products/$productId/edit"
    fun priceHistory(productId: String) = "products/$productId/price-history"
}

@Composable
fun JoyMilkNavGraph(
    navController: NavHostController,
    authRepository: AuthRepository,
    modifier: Modifier = Modifier
) {
    val startDestination = if (authRepository.isLoggedIn) Routes.DASHBOARD else Routes.LOGIN

    var userRole by remember { mutableStateOf(Constants.ROLE_STAFF) }

    LaunchedEffect(authRepository.isLoggedIn) {
        if (authRepository.isLoggedIn) {
            userRole = authRepository.getUserRole(authRepository.currentUser!!.uid)
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = { role ->
                    userRole = role
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.DASHBOARD) {
            DashboardScreen(
                userRole = userRole,
                onCreateOrder = { navController.navigate(Routes.ORDER_NEW) },
                onOrderClick = { orderId -> navController.navigate(Routes.orderDetail(orderId)) },
                onNavigateToOrders = { navController.navigate(Routes.ORDERS) },
                onNavigateToReports = { navController.navigate(Routes.REPORTS) },
                onNavigateToMore = { navController.navigate(Routes.MORE) }
            )
        }

        composable(Routes.ORDERS) {
            OrderListScreen(
                onOrderClick = { orderId -> navController.navigate(Routes.orderDetail(orderId)) },
                onCreateOrder = { navController.navigate(Routes.ORDER_NEW) },
                onNavigateToDashboard = { navController.navigate(Routes.DASHBOARD) },
                onNavigateToReports = { navController.navigate(Routes.REPORTS) },
                onNavigateToMore = { navController.navigate(Routes.MORE) }
            )
        }

        composable(Routes.ORDER_NEW) {
            OrderFormScreen(
                orderId = null,
                onOrderSaved = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.ORDER_DETAIL,
            arguments = listOf(navArgument("orderId") { type = NavType.StringType })
        ) { backStackEntry ->
            OrderDetailScreen(
                orderId = backStackEntry.arguments?.getString("orderId") ?: "",
                userRole = userRole,
                onBack = { navController.popBackStack() },
                onEditOrder = { orderId -> navController.navigate(Routes.orderEdit(orderId)) }
            )
        }

        composable(
            route = Routes.ORDER_EDIT,
            arguments = listOf(navArgument("orderId") { type = NavType.StringType })
        ) { backStackEntry ->
            OrderFormScreen(
                orderId = backStackEntry.arguments?.getString("orderId"),
                onOrderSaved = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }

        // Admin-only: Products
        composable(Routes.PRODUCTS) {
            if (userRole == Constants.ROLE_ADMIN) {
                ProductListScreen(
                    onProductClick = { productId -> navController.navigate(Routes.productEdit(productId)) },
                    onCreateProduct = { navController.navigate(Routes.PRODUCT_NEW) },
                    onPriceHistory = { productId -> navController.navigate(Routes.priceHistory(productId)) },
                    onBack = { navController.popBackStack() }
                )
            }
        }

        composable(Routes.PRODUCT_NEW) {
            if (userRole == Constants.ROLE_ADMIN) {
                ProductFormScreen(
                    productId = null,
                    onSaved = { navController.popBackStack() },
                    onBack = { navController.popBackStack() }
                )
            }
        }

        composable(
            route = Routes.PRODUCT_EDIT,
            arguments = listOf(navArgument("productId") { type = NavType.StringType })
        ) { backStackEntry ->
            if (userRole == Constants.ROLE_ADMIN) {
                ProductFormScreen(
                    productId = backStackEntry.arguments?.getString("productId"),
                    onSaved = { navController.popBackStack() },
                    onBack = { navController.popBackStack() }
                )
            }
        }

        composable(
            route = Routes.PRICE_HISTORY,
            arguments = listOf(navArgument("productId") { type = NavType.StringType })
        ) { backStackEntry ->
            if (userRole == Constants.ROLE_ADMIN) {
                PriceHistoryScreen(
                    productId = backStackEntry.arguments?.getString("productId") ?: "",
                    onBack = { navController.popBackStack() }
                )
            }
        }

        composable(Routes.REPORTS) {
            ReportScreen(
                onNavigateToDashboard = { navController.navigate(Routes.DASHBOARD) },
                onNavigateToOrders = { navController.navigate(Routes.ORDERS) },
                onNavigateToMore = { navController.navigate(Routes.MORE) }
            )
        }

        // Admin-only: Users
        composable(Routes.USERS) {
            if (userRole == Constants.ROLE_ADMIN) {
                UserListScreen(
                    onCreateUser = { navController.navigate(Routes.USER_NEW) },
                    onBack = { navController.popBackStack() }
                )
            }
        }

        composable(Routes.USER_NEW) {
            if (userRole == Constants.ROLE_ADMIN) {
                UserFormScreen(
                    onSaved = { navController.popBackStack() },
                    onBack = { navController.popBackStack() }
                )
            }
        }

        composable(Routes.MORE) {
            MoreScreen(
                userRole = userRole,
                onNavigateToProducts = { navController.navigate(Routes.PRODUCTS) },
                onNavigateToUsers = { navController.navigate(Routes.USERS) },
                onNavigateToDashboard = { navController.navigate(Routes.DASHBOARD) },
                onNavigateToOrders = { navController.navigate(Routes.ORDERS) },
                onNavigateToReports = { navController.navigate(Routes.REPORTS) },
                onLogout = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
