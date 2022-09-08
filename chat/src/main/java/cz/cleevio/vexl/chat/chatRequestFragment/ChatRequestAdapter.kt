package cz.cleevio.vexl.chat.chatRequestFragment

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import cz.cleevio.core.R
import cz.cleevio.core.model.OfferType
import cz.cleevio.core.utils.BuySellColorizer.colorizeTransactionType
import cz.cleevio.core.widget.FriendLevel
import cz.cleevio.repository.RandomUtils
import cz.cleevio.repository.model.chat.CommunicationRequest
import cz.cleevio.vexl.chat.databinding.ItemChatRequestBinding

class ChatRequestAdapter : ListAdapter<CommunicationRequest, ChatRequestAdapter.ViewHolder>(
	object : DiffUtil.ItemCallback<CommunicationRequest>() {
		override fun areItemsTheSame(oldItem: CommunicationRequest, newItem: CommunicationRequest): Boolean =
			oldItem.message.uuid == newItem.message.uuid

		override fun areContentsTheSame(oldItem: CommunicationRequest, newItem: CommunicationRequest): Boolean =
			oldItem == newItem
	}) {

	fun getItemAtIndex(index: Int): CommunicationRequest = getItem(index)

	inner class ViewHolder constructor(
		private val binding: ItemChatRequestBinding
	) : RecyclerView.ViewHolder(binding.root) {

		private lateinit var adapter: ChatRequestCommonFriendAdapter

		fun bind(item: CommunicationRequest) {
			val offer = item.offer
			val isSell = (offer?.offerType == OfferType.SELL.name && !offer.isMine)
				|| (offer?.offerType == OfferType.BUY.name && offer.isMine)

			val username = offer?.userName ?: RandomUtils.generateName()
			if (isSell) {
				colorizeTransactionType(
					binding.userName.resources.getString(R.string.marketplace_detail_user_sell, username),
					username,
					binding.userName,
					R.color.pink_100
				)
			} else {
				colorizeTransactionType(
					binding.userName.resources.getString(R.string.marketplace_detail_user_buy, username),
					username,
					binding.userName,
					R.color.green_100
				)
			}
			binding.userType.text = if (item.offer?.friendLevel == FriendLevel.FIRST_DEGREE.name) {
				binding.userType.resources.getString(R.string.marketplace_detail_friend_first)
			} else {
				binding.userType.resources.getString(R.string.marketplace_detail_friend_second)
			}
			binding.requestMessage.text = item.message.text
			item.offer?.let {
				binding.offerWidget.bind(item = it, group = item.group)
			}
			val offerList = item.offer?.commonFriends.orEmpty().map { it.contact }
			adapter.submitList(offerList)
			binding.noneCommonFriends.isVisible = offerList.isEmpty()
			binding.commonFriendsList.isVisible = offerList.isNotEmpty()
		}

		fun initAdapter() {
			adapter = ChatRequestCommonFriendAdapter()
			binding.commonFriendsList.adapter = adapter
		}
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		val viewHolder = ViewHolder(
			ItemChatRequestBinding.inflate(LayoutInflater.from(parent.context), parent, false)
		)
		viewHolder.initAdapter()
		return viewHolder
	}

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		holder.bind(getItem(position))
	}
}
