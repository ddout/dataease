<template>
  <div style="height: 100%;">
    <link-error
      v-if="showIndex===0"
      :resource-id="resourceId"
    />
    <link-pwd
      v-if="showIndex===1"
      :resource-id="resourceId"
      :user="userId"
      @fresh-token="refreshToken"
    />
    <link-view
      v-if="showIndex===2"
      :resource-id="resourceId"
      :user="userId"
    />
    <link-expire
      v-if="showIndex===3"
      :resource-id="resourceId"
      :user="userId"
    />
  </div>
</template>
<script>
import { getQueryVariable } from '@/utils/index'
import { validate } from '@/api/link'
import LinkView from './view'
import LinkError from './error'
import LinkPwd from './pwd'
import LinkExpire from './overtime'
export default {
  components: {
    LinkError, LinkPwd, LinkView, LinkExpire
  },

  data() {
    return {
      resourceId: null,
      userId: null,
      PARAMKEY: 'link',
      link: null,
      user: null,
      showIndex: -1,
      // auto2-code认证：cid=客户端应用id，rcode=临时code
      cid: null,
      rcode: null
    }
  },
  created() {
    this.loadInit()
  },
  methods: {

    loadInit() {
      this.$store.commit('setPublicLinkStatus', true)
      this.link = this.$route.query.link
      this.user = this.$route.query.user
      //auto2-code认证
      this.cid = this.$route.query.cid
      this.rcode = this.$route.query.rcode
      if (!this.link) {
        this.link = getQueryVariable(this.PARAMKEY)
      }
      if (!this.user) {
        this.user = getQueryVariable('user')
      }
      //auto2-code认证
      if (!this.cid) {
        this.cid = getQueryVariable('cid')
      }
      if (!this.rcode) {
        this.rcode = getQueryVariable('rcode')
      }
      //auto2-code认证
      if (!this.link) {
        this.showError()
        return
      }
      const params = this.user
        ? { link: encodeURIComponent(this.link), user: encodeURIComponent(this.user),cid: encodeURIComponent(this.cid), rcode: encodeURIComponent(this.rcode) }
        : { link: encodeURIComponent(this.link), cid: encodeURIComponent(this.cid), rcode: encodeURIComponent(this.rcode) }
      validate(params).then(res => {
        const { resourceId, valid, enablePwd, passPwd, expire, userId } = res.data
        this.resourceId = resourceId
        this.userId = userId
        // 如果链接无效 直接显示无效页面
        if (!valid || !resourceId) {
          this.showError()
          return
        }

        if (expire) {
          this.showExpire()
          return
        }

        if (enablePwd && !passPwd) {
          this.showPwd()
          return
        }

        this.showView()
      }).catch(() => {
        this.showError()
      })
    },
    refreshToken() {
      this.loadInit()
    },

    // 显示无效链接
    showError() {
      this.showIndex = 0
    },
    // 显示密码框
    showPwd() {
      this.showIndex = 1
    },
    // 显示仪表板
    showView() {
      this.showIndex = 2
    },
    showExpire() {
      this.showIndex = 3
    }
  }
}
</script>
